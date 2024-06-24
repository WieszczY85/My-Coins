package pl.mymc.mycoins;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import pl.mymc.mycoins.databases.MySQLDatabaseHandler;
import pl.mymc.mycoins.events.PlayerTimeTracker;
import pl.mymc.mycoins.helpers.MyCoinsDependencyManager;
import pl.mymc.mycoins.helpers.MyCoinsLogger;
import pl.mymc.mycoins.helpers.MyCoinsVersionChecker;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.configuration.file.FileConfiguration;

import java.net.URLClassLoader;
import java.sql.SQLException;

public final class My_Coin extends JavaPlugin {
    private final MyCoinsLogger logger;
    private static Economy econ = null;
    private static Permission perms = null;
    private MySQLDatabaseHandler dbHandler;
    private PlayerTimeTracker timeTracker;
    private FileConfiguration config;

    public My_Coin() {
        String fullName = getDescription().getFullName();
        String pluginName = getDescription().getRawName();
        String serverVersion = getServer().getBukkitVersion();
        this.logger = new MyCoinsLogger(fullName, serverVersion, pluginName);
    }

    @Override
    public void onLoad() {
        saveDefaultConfig();
        MyCoinsDependencyManager dm = new MyCoinsDependencyManager((URLClassLoader) getClass().getClassLoader(), logger);
        try {
            dm.loadMariaDb();
            dm.loadAdventureApi();
            dm.loadLegacy();
            dm.loadMiniMessage();
            logger.success("Zakończono ładowanie wszystkie zależności!");
        } catch (Exception e) {
            logger.err("Wystąpił błąd podczas ładowania zależności: " + e.getMessage());
        }
    }

    @Override
    public void onEnable() {
        String pluginName = getDescription().getRawName();
        String currentVersion = getDescription().getVersion();

        FileConfiguration config = getConfig();
        boolean checkForUpdates = config.getBoolean("checkForUpdates");
        boolean autoDownloadUpdates = config.getBoolean("autoDownloadUpdates");

        if (!setupService(Economy.class)) {
            logger.err("Wyłączono, ponieważ nie znaleziono zależności Vault");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        setupService(Permission.class);

        dbHandler = new MySQLDatabaseHandler(config, logger);
        try {
            dbHandler.openConnectionAndCreateTable();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        timeTracker = new PlayerTimeTracker(dbHandler, logger, config);
        getServer().getPluginManager().registerEvents(timeTracker, this);
        logger.pluginStart();
        logger.checkServerType();
        MyCoinsVersionChecker.checkVersion(pluginName, currentVersion, logger, checkForUpdates, autoDownloadUpdates);
    }
    private boolean setupService(Class<?> serviceClass) {
        RegisteredServiceProvider<?> rsp = getServer().getServicesManager().getRegistration(serviceClass);
        if (rsp == null) {
            return false;
        }
        if (serviceClass == Economy.class) {
            econ = (Economy) rsp.getProvider();
        } else if (serviceClass == Permission.class) {
            perms = (Permission) rsp.getProvider();
        }
        return rsp.getProvider() != null;
    }

    public static Economy getEconomy() {
        return econ;
    }
    public static Permission getPermissions() {
        return perms;
    }

    @Override
    public void onDisable() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                timeTracker.handlePlayerQuit(player);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        dbHandler.closeConnection();
        logger.err(getDescription().getName() +" Disabled Version "+ getDescription().getVersion());
    }
}