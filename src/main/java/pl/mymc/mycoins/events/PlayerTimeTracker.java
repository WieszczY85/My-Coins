package pl.mymc.mycoins.events;

import pl.mymc.mycoins.databases.MySQLDatabaseHandler;
import pl.mymc.mycoins.helpers.MyCoinsLogger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;

public class PlayerTimeTracker implements Listener {
    private final MySQLDatabaseHandler dbHandler;
    private final MyCoinsLogger logger;

    public PlayerTimeTracker(MySQLDatabaseHandler dbHandler, MyCoinsLogger logger) {
        this.dbHandler = dbHandler;
        this.logger = logger;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String playerName = event.getPlayer().getName();
        String playerUUID = event.getPlayer().getUniqueId().toString();
        Instant joinTime = Instant.now();
        LocalDate currentDate = LocalDate.now();
        String dateString = currentDate.toString();

        try {
            PreparedStatement statement = dbHandler.getConnection().prepareStatement("INSERT INTO `My-Coins` (player, uuid, data, joinTime) VALUES (?, ?, ?, ?);");
            statement.setString(1, playerName);
            statement.setString(2, playerUUID);
            statement.setDate(3, java.sql.Date.valueOf(dateString));
            statement.setLong(4, joinTime.getEpochSecond());
            statement.executeUpdate();
        } catch (SQLException e) {
            logger.err("Nie udało się zapisać czasu wejścia gracza do bazy danych. Szczegóły błędu: " + e.getMessage());
        }
    }



    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        String playerName = event.getPlayer().getName();
        String playerUUID = event.getPlayer().getUniqueId().toString();
        long quitTime = Instant.now().getEpochSecond();

        try {
            PreparedStatement statement = dbHandler.getConnection().prepareStatement("UPDATE `My-Coins` SET quitTime = ? WHERE uuid = ? AND quitTime IS NULL;");
            statement.setLong(1, quitTime);
            statement.setString(2, playerUUID);
            statement.executeUpdate();

            statement = dbHandler.getConnection().prepareStatement("UPDATE `My-Coins` SET totalTime = quitTime - joinTime WHERE uuid = ?;");
            statement.setString(1, playerUUID);
            statement.executeUpdate();
        } catch (SQLException e) {
            logger.err("Nie udało się zapisać czasu wyjścia gracza do bazy danych. Szczegóły błędu: " + e.getMessage());
        }
    }
}