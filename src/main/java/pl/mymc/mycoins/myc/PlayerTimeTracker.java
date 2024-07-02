package pl.mymc.mycoins.myc;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import pl.mymc.mycoins.databases.DatabaseHandler;
import pl.mymc.mycoins.helpers.MyCoinsLogger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import pl.mymc.mycoins.helpers.MyCoinsMessages;

import java.sql.*;
import java.time.Instant;
import java.time.LocalDate;

import static org.bukkit.Bukkit.getName;
import static org.bukkit.Bukkit.getServer;

public class PlayerTimeTracker implements Listener {
    private final DatabaseHandler dbHandler;
    public final FileConfiguration config;
    private final MyCoinsLogger logger;
    private final MyCoinsMessages sm;
    private final RankMultiplier rankMultiplier;

    public PlayerTimeTracker(DatabaseHandler dbHandler, MyCoinsLogger logger, FileConfiguration config, FileConfiguration localConfig) {
        this.dbHandler = dbHandler;
        this.config = config;
        this.logger = logger;
        boolean debugMode = config.getBoolean("debug");
        this.sm = new MyCoinsMessages(getName(), debugMode, localConfig, dbHandler);
        this.rankMultiplier = new RankMultiplier(config);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) throws SQLException, ClassNotFoundException {
        Player player = event.getPlayer();
        String playerName = player.getName();
        String playerUUID = player.getUniqueId().toString();
        Instant joinTime = Instant.now();

        LocalDate currentDate = LocalDate.now();
        dbHandler.savePlayerJoinTime(playerName, playerUUID, joinTime, currentDate);
        dbHandler.addNewDailyRewardEntry(playerUUID, config.getDouble("reward.daily_limit"));
        sm.sendRemainingDailyLimit(player);
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) throws SQLException, ClassNotFoundException {
        handlePlayerQuit(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) throws SQLException, ClassNotFoundException {
        handlePlayerQuit(event.getPlayer());
    }

    public void handlePlayerQuit(Player player) throws SQLException, ClassNotFoundException {
        String playerUUID = player.getUniqueId().toString();
        long quitTime = Instant.now().getEpochSecond();

        dbHandler.savePlayerQuitTime(playerUUID, quitTime);

        long sessionTimeInSeconds = dbHandler.getSessionTime(playerUUID, quitTime);
        long sessionTimeInMinutes = sessionTimeInSeconds / 60;

        double rate = config.getDouble("reward.points_rate");
        double reward = sessionTimeInMinutes  * rate;
        double dailyLimit = dbHandler.getPlayerDailyReward(playerUUID);

        if (dailyLimit + reward > config.getDouble("reward.daily_limit")) {
            reward = config.getDouble("reward.daily_limit") - dailyLimit;
            sm.sendDailyLimitMessage(player);
        }

        reward *= rankMultiplier.getMultiplier(player);

        dbHandler.handleDailyReward(playerUUID, reward);

        depositPlayerReward(player, reward);
    }

    private void depositPlayerReward(Player playerName, double reward) {
        boolean debugMode  = config.getBoolean("debug");
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return;
        }
        Economy econ = rsp.getProvider();
        econ.depositPlayer(playerName, reward);
        if(debugMode)
        {
            logger.success("Przekazano nagrodę " + reward + " dla gracza " + playerName);
        }
    }
}