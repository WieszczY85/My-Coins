package pl.mymc.mycoins.helpers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;


public class MyCoinsLogger {
    private final String fullName;
    private final String serverVersion;
    private final String plName;
    public MyCoinsLogger(String fullName, String serverVersion, String pluginName) {
        this.fullName = fullName;
        this.plName = pluginName;
        this.serverVersion = serverVersion;
    }

    public void clear(String s) {
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', s));
    }

    public void success(String s) {
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "[" + plName + "] &a&l" + s + "&r"));
    }

    public void info(String s) {
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "[" + plName + "] " + s));
    }

    public void warning(String s) {
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "[" + plName + "] &6" + s + "&r"));
    }
    public void err(String s) {
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "[" + plName + "] &c" + s + "&r"));
    }

    public void severe(String s) {
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "[" + plName + "] &c&l" + s + "&r"));
    }

    public void log(String s) {
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "[" + plName + "] " + s));
    }
    public void pluginStart() {
        clear("");
        clear("&6  __  __               _____      _           ");
        clear("&6 |  \\/  |             / ____|    (_)          ");
        clear("&6 | \\  / |_   _ ______| |     ___  _ _ __  ___ ");
        clear("&6 | |\\/| | | | |______| |    / _ \\| | '_ \\/ __|");
        clear("&6 | |  | | |_| |      | |___| (_) | | | | \\__ \\");
        clear("&6 |_|  |_|\\__, |       \\_____\\___/|_|_| |_|___/");
        clear("&6          __/ |                               ");
        clear("&6         |___/                                ");
        clear("&6 ");
        clear("&a» " + fullName + " enabled,");

    }
    public void checkServerType() {
        try {
            Class.forName("com.destroystokyo.paper.event.server.PaperServerListPingEvent");
            clear("&a       running on Paper " + serverVersion);
            clear("&a             and utilizing its optimizations");
            clear("");
        } catch (ClassNotFoundException exx) {
            try {
                Class.forName("org.bukkit.Bukkit");
                clear("&a       running on Spigot/Bukkit " + serverVersion);
                clear("&a             and utilizing its optimizations");
                clear("");
            } catch (ClassNotFoundException exxx) {
                clear("&a       running on Unknown server type " + serverVersion);
                clear("");
            }
        }
    }
}
