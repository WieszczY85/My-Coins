package pl.mymc.mycoins.databases;

import java.sql.*;
import java.time.Instant;
import java.time.LocalDate;

import org.bukkit.configuration.file.FileConfiguration;

public class MySQLDatabaseHandler {
    private Connection connection;
    private final String host, database, username, password;
    private final int port;

    public MySQLDatabaseHandler(FileConfiguration config) {
        this.host = config.getString("database.mysql.host");
        this.port = config.getInt("database.mysql.port");
        this.database = config.getString("database.mysql.name");
        this.username = config.getString("database.mysql.username");
        this.password = config.getString("database.mysql.password");
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

    public void savePlayerJoinTime(String playerName, String playerUUID, Instant joinTime, LocalDate currentDate) throws SQLException {
        PreparedStatement statement = getConnection().prepareStatement("INSERT INTO `My-Coins` (player, uuid, data, joinTime) VALUES (?, ?, ?, ?);");
        statement.setString(1, playerName);
        statement.setString(2, playerUUID);
        statement.setDate(3, java.sql.Date.valueOf(currentDate.toString()));
        statement.setLong(4, joinTime.getEpochSecond());
        statement.executeUpdate();
    }

    public void savePlayerQuitTime(String playerUUID, long quitTime) throws SQLException {
        PreparedStatement statement = getConnection().prepareStatement("UPDATE `My-Coins` SET quitTime = ? WHERE uuid = ? AND quitTime IS NULL;");
        statement.setLong(1, quitTime);
        statement.setString(2, playerUUID);
        statement.executeUpdate();

        statement = getConnection().prepareStatement("UPDATE `My-Coins` SET totalTime = quitTime - joinTime WHERE uuid = ?;");
        statement.setString(1, playerUUID);
        statement.executeUpdate();
    }

    public long getPlayerTotalTime(String playerUUID) throws SQLException {
        PreparedStatement statement = getConnection().prepareStatement("SELECT totalTime FROM `My-Coins` WHERE uuid = ?;");
        statement.setString(1, playerUUID);
        ResultSet rs = statement.executeQuery();

        if (rs.next()) {
            return rs.getLong("totalTime");
        } else {
            return 0;
        }
    }

}
