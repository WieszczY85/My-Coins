package pl.mymc.mycoins.helpers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class MyCoinsMessages {
    private final String plName;
    private final boolean debugMode;
    private final MiniMessage miniMessage;
    private final FileConfiguration localConfig;

    public MyCoinsMessages(String pluginName, boolean debugMode, FileConfiguration localConfig) {
        this.plName = pluginName;
        this.localConfig = localConfig;
        this.debugMode = debugMode;
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

}