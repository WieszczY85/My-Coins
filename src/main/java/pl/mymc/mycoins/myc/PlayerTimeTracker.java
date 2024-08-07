package pl.mymc.mycoins.myc;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import static org.bukkit.Bukkit.getServer;
import pl.mymc.mycoins.helpers.MyCoinsMessages;
import pl.mymc.mycoins.databases.DatabaseHandler;
import pl.mymc.mycoins.helpers.MyCoinsLogger;

import java.sql.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class PlayerTimeTracker implements Listener {
    private final DatabaseHandler dbHandler;
    public final FileConfiguration config;
    private final MyCoinsLogger logger;
    private final MyCoinsMessages sm;
    private final RankMultiplier rankMultiplier;
    private final Map<UUID, Boolean> handledPlayers = new HashMap<>();

    public PlayerTimeTracker(DatabaseHandler dbHandler, MyCoinsLogger logger, FileConfiguration config, FileConfiguration localConfig) {
        this.dbHandler = dbHandler;
        this.config = config;
        this.logger = logger;
        this.sm = new MyCoinsMessages(localConfig, dbHandler);
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
        try {
            if (!dbHandler.checkIfEntryExists(playerUUID)) {
                double limit = config.getDouble("reward.daily_limit");
                dbHandler.addNewDailyRewardEntry(playerUUID, limit);
                logger.debug("Utworzono brakujący wpis DailyReward");

            } else {
                logger.debug("Wpis DailyReward istnieje, nie ma potrzeby go tworzyć");
            }
        } catch (SQLException | ClassNotFoundException e) {
            logger.err("Nie udało się obsłużyć nagrody dziennych. Szczegóły błędu: " + e.getMessage());
        }
        handledPlayers.remove(player.getUniqueId());

        double remainingReward = dbHandler.getPlayerDailyReward(playerUUID);
        if (remainingReward <= 0) {
            sm.sendPlayerMessage(player, "noDailyLimit");
        } else {
            sm.sendRemainingDailyLimit(player);
        }
    }


    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) throws SQLException, ClassNotFoundException {
        Player player = event.getPlayer();
        if (!handledPlayers.containsKey(player.getUniqueId())) {
            handlePlayerQuit(player);
            handledPlayers.put(player.getUniqueId(), true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) throws SQLException, ClassNotFoundException {
        Player player = event.getPlayer();
        if (!handledPlayers.containsKey(player.getUniqueId())) {
            handlePlayerQuit(player);
            handledPlayers.put(player.getUniqueId(), true);
        }
    }
    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) throws SQLException, ClassNotFoundException {
        Player player = event.getPlayer();
        if (!handledPlayers.containsKey(player.getUniqueId())) {
            handlePlayerQuit(player);
            handledPlayers.put(player.getUniqueId(), true);
        }
    }

    public void handlePlayerQuit(Player player) throws SQLException, ClassNotFoundException {
        String playerUUID = player.getUniqueId().toString();
        long quitTime = Instant.now().getEpochSecond();

        dbHandler.savePlayerQuitTime(playerUUID, quitTime);

        long sessionTimeInSeconds = dbHandler.getSessionTime(playerUUID, quitTime);
        double sessionTimeInMinutes = sessionTimeInSeconds / 60.0;
        double rate = config.getDouble("reward.points_rate");
        double reward = sessionTimeInMinutes * rate;
        double multiplier = rankMultiplier.getMultiplier(player);
        double remainingReward = dbHandler.getPlayerDailyReward(playerUUID);

        reward *= multiplier;

        if (reward > remainingReward) {
            reward = remainingReward;
        }

        if (reward > 0 && remainingReward > 0) {
            dbHandler.handleDailyReward(playerUUID, remainingReward, reward);
            depositPlayerReward(player, reward);
        } else {
            logger.debug("Gracz " + player.getName() + " przekroczył dzienny limit nagród.");
        }
    }

    private void depositPlayerReward(Player player, double reward) {

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return;
        }
        double roundedReward = Math.round(reward * 100.0) / 100.0;

        Economy econ = rsp.getProvider();
        econ.depositPlayer(player, roundedReward);
        logger.success("Przekazano nagrodę " + roundedReward + " dla gracza " + player.getName());
    }
}