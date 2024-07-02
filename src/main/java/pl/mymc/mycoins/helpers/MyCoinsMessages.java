package pl.mymc.mycoins.helpers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import pl.mymc.mycoins.databases.DatabaseHandler;

import java.sql.SQLException;

public class MyCoinsMessages {
    private final FileConfiguration localConfig;
    private final MiniMessage minimessage;
    private final DatabaseHandler dbHandler;


    public MyCoinsMessages(String pluginName, boolean debugMode, FileConfiguration localConfig, DatabaseHandler dbHandler) {
        this.localConfig = localConfig;
        this.dbHandler = dbHandler;
        this.minimessage = MiniMessage.miniMessage();
    }

    public String getMessage(String key) {
        String prefix = localConfig.getString("prefix");
        return prefix + localConfig.getString(key);
    }

    public void sendPlayerMessage(Player player, String key) {
        String message = getMessage(key);
        Component component = minimessage.deserialize(message);
        player.sendMessage(component);
    }

    public void sendAdminMessage(String key) {
        String message = getMessage(key);
        Component component = minimessage.deserialize(message);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("mycoins.admin")) {
                player.sendMessage(component);
            }
        }
    }
    public void sendDailyLimitMessage(Player player) {
        sendPlayerMessage(player, "dailyLimit");
    }
    public void sendRemainingDailyLimit(Player player) throws SQLException, ClassNotFoundException {
        double remainingReward = dbHandler.getPlayerDailyReward(player.getUniqueId().toString());
        String message = getMessage("remainingDailyLimit").replace("%remainingReward%", String.valueOf(remainingReward));
        Component component = minimessage.deserialize(message);
        player.sendMessage(component);
    }

}