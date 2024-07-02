package pl.mymc.mycoins.databases;

import java.sql.*;
import java.time.Instant;
import java.time.LocalDate;

public interface DatabaseHandler {
    void openConnection() throws SQLException, ClassNotFoundException;

    Connection getConnection();

    void createTable();

    int incrementSessionId(String playerUUID) throws SQLException;

    void openConnectionAndCreateTable() throws SQLException, ClassNotFoundException;

    void savePlayerJoinTime(String playerName, String playerUUID, Instant joinTime, LocalDate currentDate);

    void savePlayerQuitTime(String playerUUID, long quitTime);

    double getPlayerDailyReward(String playerUUID) throws SQLException;

    void addNewDailyRewardEntry(String playerUUID, double dailyLimit);

    void updateRemainingReward(String playerUUID, double reward);

    long getSessionTime(String playerUUID, long quitTime) throws SQLException;

    void handleDailyReward(String playerUUID, double dailyLimit);

    boolean checkIfEntryExists(String playerUUID) throws SQLException;

    void closeConnection();
}


