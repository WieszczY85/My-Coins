package pl.mymc.mycoins.databases;

import org.bukkit.configuration.file.FileConfiguration;
import pl.mymc.mycoins.helpers.MyCoinsLogger;

import java.sql.*;
import java.time.Instant;
import java.time.LocalDate;

public class SQLiteDatabaseHandler implements DatabaseHandler {
    private Connection connection;
    private final String url;
    private final MyCoinsLogger logger;
    private final boolean debugMode;

    public SQLiteDatabaseHandler(FileConfiguration config, MyCoinsLogger logger) {
        this.url = "jdbc:sqlite:plugins/My-Coins/myc_database.db";
        this.debugMode = config.getBoolean("debug");
        this.logger = logger;
    }

    public void openConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            return;
        }

        connection = DriverManager.getConnection(url);
    }

    public Connection getConnection() {
        return connection;
    }

    public boolean isConnected() {
        try {
            return (connection == null || connection.isClosed());
        } catch (SQLException e) {
            logger.err("Nie udało się ponownie połączyć z bazą danych!" + e.getMessage());
            return true;
        }
    }

    public void createTable() {
        String createTable = "CREATE TABLE IF NOT EXISTS My-Coins (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "player TEXT NOT NULL," +
                "uuid TEXT NOT NULL," +
                "sessionId INTEGER NOT NULL," +
                "data DATE NOT NULL DEFAULT CURRENT_DATE," +
                "joinTime INTEGER NOT NULL," +
                "quitTime INTEGER DEFAULT NULL," +
                "totalTime INTEGER DEFAULT NULL" +
                ");";

        String createDailyRewardsTable = "CREATE TABLE IF NOT EXISTS DailyRewards (" +
                "uuid TEXT NOT NULL," +
                "date DATE NOT NULL DEFAULT CURRENT_DATE," +
                "remainingReward REAL NOT NULL," +
                "PRIMARY KEY (uuid, date)" +
                ");";

        try (Statement statement = getConnection().createStatement()) {
            statement.execute(createTable);
            statement.execute(createDailyRewardsTable);
        } catch (SQLException e) {
            logger.err("Nie udało się wykonać operacji na bazie danych!" + e.getMessage());
        }
    }

    public void openConnectionAndCreateTable() {
        try {
            openConnection();
            logger.success("Połączono z bazą danych SQLite!");
            createTable();
        } catch (SQLException e) {
            logger.err("Nie udało się połączyć z bazą danych!" + e.getMessage());
        }
    }

    public void savePlayerJoinTime(String playerName, String playerUUID, Instant joinTime, LocalDate currentDate) {
        String sql = "INSERT INTO `My-Coins` (player, uuid, sessionId, data, joinTime) VALUES (?, ?, ?, ?, ?);";

        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setString(1, playerName);
            statement.setString(2, playerUUID);
            statement.setInt(3, incrementSessionId(playerUUID));
            statement.setDate(4, java.sql.Date.valueOf(currentDate));
            statement.setLong(5, joinTime.getEpochSecond());
            statement.executeUpdate();
            if(debugMode) {
                logger.debug("Zapisano czas wejścia gracza " + playerName + " (" + playerUUID + ") do bazy danych. Czas wejścia: " + joinTime + ", Data: " + currentDate);
            }
        } catch (SQLException e) {
            logger.err("Nie udało się zapisać czasu wejścia gracza do bazy danych. Szczegóły błędu: " + e.getMessage());
        }
    }

    public int incrementSessionId(String playerUUID) throws SQLException {
        String sql = "SELECT MAX(sessionId) AS maxSessionId FROM `My-Coins` WHERE uuid = ?;";

        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setString(1, playerUUID);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("maxSessionId") + 1;
                } else {
                    return 1;
                }
            }
        }
    }

    public void savePlayerQuitTime(String playerUUID, long quitTime) {
        try (PreparedStatement statement = getConnection().prepareStatement("UPDATE `My-Coins` SET quitTime = ? WHERE uuid = ? AND sessionId = (SELECT MAX(sessionId) FROM `My-Coins` WHERE uuid = ?) AND quitTime IS NULL;")) {
            statement.setLong(1, quitTime);
            statement.setString(2, playerUUID);
            statement.setString(3, playerUUID);
            statement.executeUpdate();
            if(debugMode) {
                logger.debug("Zapisano czas wyjścia gracza o UUID: " + playerUUID + " do bazy danych. Czas wyjścia: " + quitTime);
            }
        }catch (SQLException e) {
            logger.err("Nie udało się zapisać czasu wyjścia gracza do bazy danych. Szczegóły błędu: " + e.getMessage());
        }

        long totalTime = 0;
        try (PreparedStatement statement = getConnection().prepareStatement("UPDATE `My-Coins` SET totalTime = quitTime - joinTime WHERE uuid = ? AND sessionId = (SELECT MAX(sessionId) FROM `My-Coins` WHERE uuid = ?);")) {
            statement.setString(1, playerUUID);
            statement.setString(2, playerUUID);
            statement.executeUpdate();
        }catch (SQLException e) {
            logger.err("Nie udało się zapisać czasu online gracza do bazy danych. Szczegóły błędu: " + e.getMessage());
        }

        try (PreparedStatement statement = getConnection().prepareStatement("SELECT totalTime FROM `My-Coins` WHERE uuid = ? AND sessionId = (SELECT MAX(sessionId) FROM `My-Coins` WHERE uuid = ?);")) {
            statement.setString(1, playerUUID);
            statement.setString(2, playerUUID);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    totalTime = rs.getLong("totalTime");
                }
            }
        }catch (SQLException e) {
            logger.err("Nie udało się pobrać czasu online z bazy danych. Szczegóły błędu: " + e.getMessage());
        }

        if(debugMode) {
            logger.debug("Zapisano czas online gracza o UUID " + playerUUID + " do bazy danych. Całkowity czas online: " + totalTime);
        }
    }

    public double getPlayerDailyReward(String playerUUID) throws SQLException {
        try (PreparedStatement statement = getConnection().prepareStatement("SELECT remainingReward FROM `DailyRewards` WHERE uuid = ? AND date = ?;")) {
            statement.setString(1, playerUUID);
            statement.setDate(2, java.sql.Date.valueOf(LocalDate.now().toString()));
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    if(debugMode) {
                        logger.debug("Pobrano pozostąłą ilość dostępnej nagrody dla gracza o UUID: " + playerUUID + " z bazy danych: " + rs.getDouble("remainingReward"));
                    }
                    return rs.getDouble("remainingReward");
                } else {
                    return 0;
                }
            }catch (SQLException e) {
                logger.err("Nie udało się pobrać dziennego limitu nagród z bazy danych. Szczegóły błędu: " + e.getMessage());
            }
        }
        return 0;
    }

    public void addNewDailyRewardEntry(String playerUUID, double dailyLimit) {
        try (PreparedStatement statement = getConnection().prepareStatement("INSERT INTO `DailyRewards` (uuid, date, remainingReward) VALUES (?, ?, ?);")) {
            statement.setString(1, playerUUID);
            statement.setDate(2, java.sql.Date.valueOf(LocalDate.now().toString()));
            statement.setDouble(3, dailyLimit);
            statement.executeUpdate();
            if(debugMode) {
                logger.debug("Dodano wpis do DailyRewards dla gracza o UUID: " + playerUUID + ", z data: " + java.sql.Date.valueOf(LocalDate.now().toString()));
                logger.debug("Ustawiono wartość remainingReward na: " + dailyLimit);
            }
        } catch (SQLException e) {
            logger.err("Nie udało się dodać nowego wpisu do tabeli DailyRewards. Szczegóły błędu: " + e.getMessage());
        }
    }


    public void updateRemainingReward(String playerUUID, double reward) throws SQLException {

        try (PreparedStatement statement = getConnection().prepareStatement("UPDATE `DailyRewards` SET remainingReward = remainingReward - ? WHERE uuid = ? AND date = ?;")) {
            statement.setDouble(1, reward);
            statement.setString(2, playerUUID);
            statement.setDate(3, java.sql.Date.valueOf(LocalDate.now().toString()));
            statement.executeUpdate();
            if(debugMode) {
                logger.debug("Zaktualizowano wpis do DailyRewards dla gracza o UUID: " + playerUUID + ", z data: " + java.sql.Date.valueOf(LocalDate.now().toString()));
                logger.debug("Ustawiono wartość remainingReward na: " + reward);
            }
        } catch (SQLException e) {
            logger.err("Nie udało się zaktualizować pozostałej nagrody w bazie danych. Szczegóły błędu: " + e.getMessage());
        }
    }

    public long getSessionTime(String playerUUID, long quitTime) throws SQLException {
        try (PreparedStatement statement = getConnection().prepareStatement("SELECT totalTime FROM `My-Coins` WHERE uuid = ? AND quitTime = ?;")) {
            statement.setString(1, playerUUID);
            statement.setLong(2, quitTime);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("totalTime");
                } else {
                    return 0;
                }
            }
        }
    }

    public void handleDailyReward(String playerUUID, double dailyLimit, double reward) {
        try {
            if (checkIfEntryExists(playerUUID)) {
                updateRemainingReward(playerUUID, reward);
            } else {
                addNewDailyRewardEntry(playerUUID, dailyLimit);
            }
        } catch (SQLException e) {
            logger.err("Nie udało się obsłużyć nagrody dziennych. Szczegóły błędu: " + e.getMessage());
        }
    }

    public boolean checkIfEntryExists(String playerUUID) throws SQLException {
        try (PreparedStatement statement = getConnection().prepareStatement("SELECT 1 FROM `DailyRewards` WHERE uuid = ? AND date = ?;")) {
            statement.setString(1, playerUUID);
            statement.setDate(2, java.sql.Date.valueOf(LocalDate.now().toString()));
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.info("Połączenie z bazą danych zamknięte!");
            }
        } catch (SQLException e) {
            logger.err("Nie udało się zamknąć połączenia z bazą danych! Szczegóły błędu: " + e.getMessage());
        }
    }
}
