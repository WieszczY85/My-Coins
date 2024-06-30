package pl.mymc.mycoins.helpers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import pl.mymc.mycoins.databases.MySQLDatabaseHandler;

import java.sql.SQLException;

public class MyCoinsMessages {
    private final MiniMessage miniMessage;
    private final FileConfiguration localConfig;
    private final MySQLDatabaseHandler dbHandler;

    public MyCoinsMessages(String pluginName, boolean debugMode, FileConfiguration localConfig, MySQLDatabaseHandler dbHandler) {
        this.localConfig = localConfig;
        this.dbHandler = dbHandler;
        this.miniMessage = MiniMessage.miniMessage();
    }

    public String getMessage(String key) {
        String prefix = localConfig.getString("prefix");
        return prefix + localConfig.getString(key);
    }

    public void sendPlayerMessage(Player player, String key) {
        String message = getMessage(key);
        Component component = miniMessage.deserialize(message);
        player.sendMessage(component);
    }

    public void sendAdminMessage(String key) {
        String message = getMessage(key);
        Component component = miniMessage.deserialize(message);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("mycoins.admin")) {
                player.sendMessage(component);
            }
        }
    }
    public void sendDailyLimitMessage(Player player) {
        sendPlayerMessage(player, "dailyLimit");
    }
    public void sendRemainingDailyLimit(Player player) throws SQLException {
        double remainingReward = dbHandler.getPlayerDailyReward(player.getUniqueId().toString());
        String message = getMessage("remainingDailyLimit").replace("%remainingReward%", String.valueOf(remainingReward));
        Component component = miniMessage.deserialize(message);
        player.sendMessage(component);
    }

}