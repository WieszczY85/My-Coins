package pl.mymc.mycoins.databases;

import java.sql.*;
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
}
