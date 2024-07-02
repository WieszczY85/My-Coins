package pl.mymc.mycoins.databases;

import java.sql.*;
import java.time.Instant;
import java.time.LocalDate;

public interface DatabaseHandler {
    void openConnection() throws SQLException, ClassNotFoundException;

    Connection getConnection();

    boolean isConnected();

    void createTable();

    int incrementSessionId(String playerUUID) throws SQLException, ClassNotFoundException;

    void openConnectionAndCreateTable() throws SQLException, ClassNotFoundException;

    void savePlayerJoinTime(String playerName, String playerUUID, Instant joinTime, LocalDate currentDate) throws SQLException, ClassNotFoundException;

    void savePlayerQuitTime(String playerUUID, long quitTime) throws SQLException, ClassNotFoundException;

    double getPlayerDailyReward(String playerUUID) throws SQLException, ClassNotFoundException;

    void addNewDailyRewardEntry(String playerUUID, double dailyLimit) throws SQLException, ClassNotFoundException;

    void updateRemainingReward(String playerUUID, double reward) throws SQLException, ClassNotFoundException;

    long getSessionTime(String playerUUID, long quitTime) throws SQLException, ClassNotFoundException;

    void handleDailyReward(String playerUUID, double dailyLimit);

    boolean checkIfEntryExists(String playerUUID) throws SQLException, ClassNotFoundException;

    void closeConnection();
}


