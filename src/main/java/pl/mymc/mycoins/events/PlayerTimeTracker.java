package pl.mymc.mycoins.events;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.file.FileConfiguration;
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
    private final MyCoinsLogger logger;
    public final FileConfiguration config;

    public PlayerTimeTracker(MySQLDatabaseHandler dbHandler, MyCoinsLogger logger, FileConfiguration config) {
        this.dbHandler = dbHandler;
        this.logger = logger;
        this.config = config;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String playerName = event.getPlayer().getName();
        String playerUUID = event.getPlayer().getUniqueId().toString();
        Instant joinTime = Instant.now();
        LocalDate currentDate = LocalDate.now();

        try {
            dbHandler.savePlayerJoinTime(playerName, playerUUID, joinTime, currentDate);
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
            dbHandler.savePlayerQuitTime(playerUUID, quitTime);

            long totalTime = dbHandler.getPlayerTotalTime(playerUUID);
            double rate = config.getDouble("reward.points_rate");
            double reward = totalTime * rate;

            RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp == null) {
                return;
            }
            Economy econ = rsp.getProvider();
            econ.depositPlayer(playerName, reward);
        } catch (SQLException e) {
            logger.err("Nie udało się zapisać czasu wyjścia gracza do bazy danych. Szczegóły błędu: " + e.getMessage());
        }
    }
}