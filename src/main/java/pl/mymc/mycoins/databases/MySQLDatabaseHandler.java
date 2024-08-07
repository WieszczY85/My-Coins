package pl.mymc.mycoins.databases;

import java.sql.*;
import java.time.Instant;
import java.time.LocalDate;

import org.bukkit.configuration.file.FileConfiguration;
import pl.mymc.mycoins.helpers.MyCoinsLogger;

public class MySQLDatabaseHandler implements DatabaseHandler {
    private Connection connection;
    private final String host, database, username, password;
    private final int port;
    private final MyCoinsLogger logger;
    private final FileConfiguration config;

    public MySQLDatabaseHandler(FileConfiguration config, MyCoinsLogger logger) {
        this.config = config;
        this.host = this.config.getString("database.mysql.host");
        this.port = this.config.getInt("database.mysql.port");
        this.database = this.config.getString("database.mysql.name");
        this.username = this.config.getString("database.mysql.username");
        this.password = this.config.getString("database.mysql.password");
        this.logger = logger;



    }

    public void openConnection() throws SQLException, ClassNotFoundException {
        if (connection != null && !connection.isClosed()) {
            return;
        }

        synchronized (this) {
            if (connection != null && !connection.isClosed()) {
                return;
            }
            Class.forName("org.mariadb.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:mariadb://" + this.host+ ":" + this.port + "/" + this.database, this.username, this.password);
        }
    }
    public boolean isConnected() {
        try {
            return (connection == null || connection.isClosed());
        } catch (SQLException e) {
            logger.err("Nie udało się ponownie połączyć z bazą danych!" + e.getMessage());
            return true;
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void createTable() {
        try (Statement statement = getConnection().createStatement()) {
            logger.debug("Sprawdzanie tabeli My-Coins w bazie danych");

            String createTable = "CREATE TABLE IF NOT EXISTS `My-Coins` (" +
                    "`id` int(11) NOT NULL AUTO_INCREMENT," +
                    "`player` varchar(255) NOT NULL," +
                    "`uuid` varchar(255) NOT NULL," +
                    "`sessionId` int(11) NOT NULL," +
                    "`data` date NOT NULL DEFAULT curdate()," +
                    "`joinTime` time NOT NULL," +
                    "`quitTime` time DEFAULT NULL," +
                    "`totalTime` time DEFAULT NULL," +
                    "PRIMARY KEY (`id`)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;";

            statement.execute(createTable);
            logger.debug("Zakończono operacje na tabeli My-Coins");

            logger.debug("Sprawdzanie tabeli DailyRewards w bazie danych");

            String createDailyRewardsTable = "CREATE TABLE IF NOT EXISTS `DailyRewards` (" +
                    "`uuid` varchar(255) NOT NULL," +
                    "`date` date NOT NULL DEFAULT curdate()," +
                    "`remainingReward` double NOT NULL," +
                    "PRIMARY KEY (`uuid`, `date`)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;";

            statement.execute(createDailyRewardsTable);
            logger.debug("Zakończono operacje na tabeli DailyRewards");

        }catch (SQLException e) {
            logger.err("Nie udało się wykonać operacji na bazie danych!" + e.getMessage());
        }
    }

    public void openConnectionAndCreateTable() {
        try {
            openConnection();
        }catch (SQLException | ClassNotFoundException e) {
            logger.err("Nie udało się połączyć z bazą danych!" + e.getMessage());
        }
        logger.success("Połączono z bazą danych MySQL/MariaDB!");
        createTable();
    }

    public int incrementSessionId(String playerUUID) throws SQLException, ClassNotFoundException {
        if (isConnected()) {
            openConnection();
        }
        try (PreparedStatement statement = getConnection().prepareStatement("SELECT MAX(sessionId) AS maxSessionId FROM `My-Coins` WHERE uuid = ?;")) {
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
    public void savePlayerJoinTime(String playerName, String playerUUID, Instant joinTime, LocalDate currentDate) throws SQLException, ClassNotFoundException {
        if (isConnected()) {
            openConnection();
        }
        try (PreparedStatement statement = getConnection().prepareStatement("INSERT INTO `My-Coins` (player, uuid, sessionId, data, joinTime) VALUES (?, ?, ?, ?, ?);")) {
            int sessionId = incrementSessionId(playerUUID);
            statement.setString(1, playerName);
            statement.setString(2, playerUUID);
            statement.setInt(3, sessionId);
            statement.setDate(4, java.sql.Date.valueOf(currentDate.toString()));
            statement.setLong(5, joinTime.getEpochSecond());
            statement.executeUpdate();
            logger.debug("Zapisano czas wejścia gracza " + playerName + " (" + playerUUID + ") do bazy danych. Czas wejścia: " + joinTime + ", Data: " + currentDate);

        } catch (SQLException e) {
            logger.err("Nie udało się zapisać czasu wejścia gracza do bazy danych. Szczegóły błędu: " + e.getMessage());
        }
    }

    public void savePlayerQuitTime(String playerUUID, long quitTime) throws SQLException, ClassNotFoundException {
        if (isConnected()) {
            openConnection();
        }
        try (PreparedStatement statement = getConnection().prepareStatement("UPDATE `My-Coins` SET quitTime = ? WHERE uuid = ? AND sessionId = (SELECT MAX(sessionId) FROM `My-Coins` WHERE uuid = ?) AND quitTime IS NULL;")) {
            statement.setLong(1, quitTime);
            statement.setString(2, playerUUID);
            statement.setString(3, playerUUID);
            statement.executeUpdate();
            logger.debug("Zapisano czas wyjścia gracza o UUID: " + playerUUID + " do bazy danych. Czas wyjścia: " + quitTime);

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

        logger.debug("Zapisano czas online gracza o UUID: " + playerUUID + " do bazy danych. Czas tej sesji online: " + totalTime);

    }

    public double getPlayerDailyReward(String playerUUID) throws SQLException, ClassNotFoundException {
        if (isConnected()) {
            openConnection();
        }
        try (PreparedStatement statement = getConnection().prepareStatement("SELECT remainingReward FROM `DailyRewards` WHERE uuid = ? AND date = ?;")) {
            statement.setString(1, playerUUID);
            statement.setDate(2, java.sql.Date.valueOf(LocalDate.now().toString()));
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    logger.debug("Pobrano ilość dostępnej nagrody dla gracza o UUID: " + playerUUID + " z datą: " +java.sql.Date.valueOf(LocalDate.now().toString()) + ": " + rs.getDouble("remainingReward"));

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

    public void addNewDailyRewardEntry(String playerUUID, double dailyLimit) throws SQLException, ClassNotFoundException {
        if (isConnected()) {
            openConnection();
        }
        try (PreparedStatement statement = getConnection().prepareStatement("INSERT INTO `DailyRewards` (uuid, date, remainingReward) VALUES (?, ?, ?);")) {
            statement.setString(1, playerUUID);
            statement.setDate(2, java.sql.Date.valueOf(LocalDate.now().toString()));
            statement.setDouble(3, dailyLimit);
            statement.executeUpdate();
            logger.debug("Dodano wpis do DailyRewards dla gracza o UUID: " + playerUUID + ", z data: " + java.sql.Date.valueOf(LocalDate.now().toString()));
            logger.debug("Ustawiono nową wartość remainingReward na: " + dailyLimit);

        } catch (SQLException e) {
            logger.err("Nie udało się dodać nowego wpisu do tabeli DailyRewards. Szczegóły błędu: " + e.getMessage());
        }
    }


    public void updateRemainingReward(String playerUUID, double reward) throws SQLException, ClassNotFoundException {
        if (isConnected()) {
            openConnection();
        }
        try (PreparedStatement statement = getConnection().prepareStatement("UPDATE `DailyRewards` SET remainingReward = remainingReward - ? WHERE uuid = ? AND date = ?;")) {
            statement.setDouble(1, reward);
            statement.setString(2, playerUUID);
            statement.setDate(3, java.sql.Date.valueOf(LocalDate.now().toString()));
            statement.executeUpdate();
            double dailyReward = getPlayerDailyReward(playerUUID);
            double remainingDailyReward = dailyReward - reward;
            logger.debug("Zaktualizowano wpis do DailyRewards dla gracza o UUID: " + playerUUID + ", z data: " + java.sql.Date.valueOf(LocalDate.now().toString()));
            logger.debug("Ustawiono zaktualizowaną wartość remainingReward na: " + remainingDailyReward);

        } catch (SQLException e) {
            logger.err("Nie udało się zaktualizować pozostałej nagrody w bazie danych. Szczegóły błędu: " + e.getMessage());
        }
    }

    public long getSessionTime(String playerUUID, long quitTime) throws SQLException, ClassNotFoundException {
        if (isConnected()) {
            openConnection();
        }
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
                double limit = config.getDouble("reward.daily_limit");
                addNewDailyRewardEntry(playerUUID, limit);
            }
        } catch (SQLException | ClassNotFoundException e) {
            logger.err("Nie udało się obsłużyć nagrody dziennych. Szczegóły błędu: " + e.getMessage());
        }
    }

    public boolean checkIfEntryExists(String playerUUID) throws SQLException, ClassNotFoundException {
        if (isConnected()) {
            openConnection();
        }
        logger.debug("Sprawdzanie czy istnieje wpis dla DailyRewards dla UUID: " + playerUUID + " i daty: " + java.sql.Date.valueOf(LocalDate.now().toString()));
        try (PreparedStatement statement = getConnection().prepareStatement("SELECT 1 FROM `DailyRewards` WHERE uuid = ? AND date = ?;")) {
            statement.setString(1, playerUUID);
            statement.setDate(2, java.sql.Date.valueOf(LocalDate.now().toString()));
            try (ResultSet rs = statement.executeQuery()) {
                boolean entryExists = rs.next();
                if(entryExists){
                    logger.debug("Odpowiedź bazy: istnieje wpisz w DailyRewards");
                }else{
                    logger.debug("Odpowiedź bazy: nie istnieje wpisz w DailyRewards");
                }
                return entryExists;
            }catch (SQLException e) {
                logger.err("Nie udało się sprawdzić wpisu DailyRewards! Szczegóły błędu: " + e.getMessage());
                return false;
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