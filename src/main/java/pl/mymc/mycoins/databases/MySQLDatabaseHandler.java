package pl.mymc.mycoins.databases;

import java.sql.*;
import java.time.Instant;
import java.time.LocalDate;

import org.bukkit.configuration.file.FileConfiguration;
import pl.mymc.mycoins.helpers.MyCoinsLogger;

public class MySQLDatabaseHandler {
    private Connection connection;
    private final String host, database, username, password;
    private final int port;
    private final MyCoinsLogger logger;

    public MySQLDatabaseHandler(FileConfiguration config, MyCoinsLogger logger) {
        this.host = config.getString("database.mysql.host");
        this.port = config.getInt("database.mysql.port");
        this.database = config.getString("database.mysql.name");
        this.username = config.getString("database.mysql.username");
        this.password = config.getString("database.mysql.password");
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

    public Connection getConnection() {
        return connection;
    }

    public void createTable() throws SQLException {
        try (Statement statement = getConnection().createStatement()) {
            logger.info("Sprawdzanie tabeli My-Coins w bazie danych");
            String createTable = "CREATE TABLE IF NOT EXISTS `My-Coins` (" +
                    "`id` int(11) NOT NULL AUTO_INCREMENT," +
                    "`player` varchar(255) NOT NULL," +
                    "`uuid` varchar(255) NOT NULL," +
                    "`data` date NOT NULL DEFAULT curdate()," +
                    "`joinTime` time NOT NULL," +
                    "`quitTime` time DEFAULT NULL," +
                    "`totalTime` time DEFAULT NULL," +
                    "PRIMARY KEY (`id`)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;";

            statement.execute(createTable);
            logger.info("Zakończono operacje na tabeli");
        }catch (SQLException e) {
            logger.err("Nie udało się wykonać operacji na bazie danych!" + e.getMessage());
        }
    }
    public void openConnectionAndCreateTable() throws SQLException, ClassNotFoundException {
        try {
            openConnection();
        }catch (SQLException | ClassNotFoundException e) {
            logger.err("Nie udało się połączyć z bazą danych!" + e.getMessage());
        }
        logger.success("Połączono z bazą danych!");
        createTable();
    }


    public void savePlayerJoinTime(String playerName, String playerUUID, Instant joinTime, LocalDate currentDate) {
        try (PreparedStatement statement = getConnection().prepareStatement("INSERT INTO `My-Coins` (player, uuid, data, joinTime) VALUES (?, ?, ?, ?);")) {
            statement.setString(1, playerName);
            statement.setString(2, playerUUID);
            statement.setDate(3, java.sql.Date.valueOf(currentDate.toString()));
            statement.setLong(4, joinTime.getEpochSecond());
            statement.executeUpdate();
        } catch (SQLException e) {
            logger.err("Nie udało się zapisać czasu wejścia gracza do bazy danych. Szczegóły błędu: " + e.getMessage());
        }
    }

    public void savePlayerQuitTime(String playerUUID, long quitTime) {
        try (PreparedStatement statement = getConnection().prepareStatement("UPDATE `My-Coins` SET quitTime = ? WHERE uuid = ? AND quitTime IS NULL;")) {
            statement.setLong(1, quitTime);
            statement.setString(2, playerUUID);
            statement.executeUpdate();
        }catch (SQLException e) {
            logger.err("Nie udało się zapisać czasu wyjścia gracza do bazy danych. Szczegóły błędu: " + e.getMessage());
        }

        try (PreparedStatement statement = getConnection().prepareStatement("UPDATE `My-Coins` SET totalTime = quitTime - joinTime WHERE uuid = ?;")) {
            statement.setString(1, playerUUID);
            statement.executeUpdate();
        }catch (SQLException e) {
            logger.err("Nie udało się zapisać czasu online gracza do bazy danych. Szczegóły błędu: " + e.getMessage());
        }
    }

    public long getPlayerTotalTime(String playerUUID) throws SQLException {
        try (PreparedStatement statement = getConnection().prepareStatement("SELECT totalTime FROM `My-Coins` WHERE uuid = ?;")) {
            statement.setString(1, playerUUID);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("totalTime");
                } else {
                    return 0;
                }
            }catch (SQLException e) {
                logger.err("Nie udało się pobrać czasu online z bazy danych. Szczegóły błędu: " + e.getMessage());
            }
        }
        return 0;
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