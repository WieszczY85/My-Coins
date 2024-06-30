package pl.mymc.mycoins.myc;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import pl.mymc.mycoins.databases.MySQLDatabaseHandler;
import pl.mymc.mycoins.helpers.MyCoinsLogger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.sql.*;
import java.time.Instant;
import java.time.LocalDate;

import static org.bukkit.Bukkit.getServer;

public class PlayerTimeTracker implements Listener {
    private final MySQLDatabaseHandler dbHandler;
    public final FileConfiguration config;

    public PlayerTimeTracker(MySQLDatabaseHandler dbHandler, MyCoinsLogger logger, FileConfiguration config) {
        this.dbHandler = dbHandler;
        this.config = config;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String playerName = event.getPlayer().getName();
        String playerUUID = event.getPlayer().getUniqueId().toString();
        Instant joinTime = Instant.now();

        LocalDate currentDate = LocalDate.now();
        dbHandler.savePlayerJoinTime(playerName, playerUUID, joinTime, currentDate);

    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) throws SQLException {
        handlePlayerQuit(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) throws SQLException {
        handlePlayerQuit(event.getPlayer());
    }

    public void handlePlayerQuit(Player player) throws SQLException {
        String playerName = player.getName();
        String playerUUID = player.getUniqueId().toString();
        long quitTime = Instant.now().getEpochSecond();

        dbHandler.savePlayerQuitTime(playerUUID, quitTime);

        long totalTime = dbHandler.getPlayerTotalTime(playerUUID);
        double rate = config.getDouble("reward.points_rate");
        double reward = totalTime * rate;

        depositPlayerReward(playerName, reward);
    }

    private void depositPlayerReward(String playerName, double reward) {
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return;
        }
        Economy econ = rsp.getProvider();
        econ.depositPlayer(playerName, reward);
    }
}