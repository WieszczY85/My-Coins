package pl.mymc.mycoins.helpers;

public class MyCoinsServerChecker {
    private final String serverVersion;

    public MyCoinsServerChecker(String serverVersion) {
        this.serverVersion = serverVersion;
    }

    public String checkServerType() {
        try {
            Class.forName("com.destroystokyo.paper.event.server.PaperServerListPingEvent");
            return "Paper";
        } catch (ClassNotFoundException e) {
            try {
                Class.forName("org.spigotmc.SpigotConfig");
                return "Spigot";
            } catch (ClassNotFoundException ex) {
                try {
                    Class.forName("org.bukkit.Bukkit");
                    return "Bukkit";
                } catch (ClassNotFoundException exx) {
                    return "Unknown";
                }
            }
        }
    }
}