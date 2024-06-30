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
import pl.mymc.mycoins.helpers.MyCoinsMessages;

import java.sql.*;
import java.time.Instant;
import java.time.LocalDate;

import static org.bukkit.Bukkit.getName;
import static org.bukkit.Bukkit.getServer;

public class PlayerTimeTracker implements Listener {
    private final MySQLDatabaseHandler dbHandler;
    public final FileConfiguration config;
    private final MyCoinsLogger logger;
    private final MyCoinsMessages sm;

    public PlayerTimeTracker(MySQLDatabaseHandler dbHandler, MyCoinsLogger logger, FileConfiguration config, FileConfiguration localConfig) {
        this.dbHandler = dbHandler;
        this.config = config;
        this.logger = logger;
        boolean debugMode = config.getBoolean("debug");
        this.sm = new MyCoinsMessages(getName(), debugMode, localConfig);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) throws SQLException {
        String playerName = event.getPlayer().getName();
        String playerUUID = event.getPlayer().getUniqueId().toString();
        Instant joinTime = Instant.now();

        LocalDate currentDate = LocalDate.now();
        dbHandler.savePlayerJoinTime(playerName, playerUUID, joinTime, currentDate);
        dbHandler.addNewDailyRewardEntry(playerUUID, config.getDouble("reward.daily_limit"));
        double remainingReward = dbHandler.getPlayerDailyReward(playerUUID);
        event.getPlayer().sendMessage("Witaj na serwerze! Dzisiaj możesz jeszcze zdobyć " + remainingReward + " punktów nagrody.");
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
        String playerUUID = player.getUniqueId().toString();
        long quitTime = Instant.now().getEpochSecond();

        dbHandler.savePlayerQuitTime(playerUUID, quitTime);

        long totalTime = dbHandler.getPlayerTotalTime(playerUUID);
        double rate = config.getDouble("reward.points_rate");
        double reward = totalTime * rate;
        double dailyLimit = config.getDouble("reward.daily_limit");

        if (dbHandler.getPlayerDailyReward(playerUUID) + reward > dailyLimit) {
            reward = dailyLimit - dbHandler.getPlayerDailyReward(playerUUID);
            sm.sendDailyLimitMessage(player);
        }

        dbHandler.updateRemainingReward(playerUUID, reward);
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