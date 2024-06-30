package pl.mymc.mycoins;

import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.configuration.file.FileConfiguration;

import pl.mymc.mycoins.databases.MySQLDatabaseHandler;
import pl.mymc.mycoins.myc.PlayerTimeTracker;
import pl.mymc.mycoins.helpers.MyCoinsDependencyManager;
import pl.mymc.mycoins.helpers.MyCoinsLogger;
import pl.mymc.mycoins.helpers.MyCoinsVersionChecker;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import java.io.File;
import java.io.IOException;
import java.net.URLClassLoader;
import java.sql.SQLException;


public final class My_Coin extends JavaPlugin {

    //TODO:
    // * Debug - przenieść część instniejących logów (zmniejszenie spamu w konsoli)
    // * Dodać multiplikacje dla rang + weryfikacja rang z Vault
    // * Dodać sprawdzanie czy gracz się porusza i jeśli nie to czy ma AFK
    // * Dodać obsługe AFK z EternalsCore i EssentialsX

    private final MyCoinsLogger logger;
    private static Economy econ = null;
    private static Permission perms = null;
    private MySQLDatabaseHandler dbHandler;
    private PlayerTimeTracker timeTracker;
    private final FileConfiguration config;
    private final boolean debugMode;
    private final boolean checkForUpdates;
    private final boolean autoDownloadUpdates;
    private FileConfiguration localConfig;

    public My_Coin() {
        String fullName = getDescription().getFullName();
        String pluginName = getDescription().getRawName();
        String serverVersion = getServer().getBukkitVersion();
        this.config = getConfig();
        this.debugMode = config.getBoolean("debug");
        this.checkForUpdates = config.getBoolean("checkForUpdates");
        this.autoDownloadUpdates = config.getBoolean("autoDownloadUpdates");
        this.logger = new MyCoinsLogger(fullName, serverVersion, pluginName, debugMode);
    }

    @Override
    public void onLoad() {
        saveDefaultConfig();
        createLocalConfig();
        MyCoinsDependencyManager dm = new MyCoinsDependencyManager((URLClassLoader) getClass().getClassLoader(), logger);
        try {
            dm.loadMariaDb();
            dm.loadAdventureApi();
            dm.loadLegacy();
            dm.loadMiniMessage();
            if (debugMode) {
                logger.success("Zakończono ładowanie wszystkie zależności!");
            }
        } catch (Exception e) {
            logger.err("Wystąpił błąd podczas ładowania zależności: " + e.getMessage());
        }
    }

    @Override
    public void onEnable() {
        String pluginName = getDescription().getRawName();
        String currentVersion = getDescription().getVersion();

        if (!setupService(Economy.class)) {
            logger.err("Wyłączono, ponieważ nie znaleziono zależności Vault");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        setupService(Permission.class);

        dbHandler = new MySQLDatabaseHandler(config, logger);
        try {
            dbHandler.openConnectionAndCreateTable();
        } catch (SQLException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        timeTracker = new PlayerTimeTracker(dbHandler, logger, config, localConfig);
        getServer().getPluginManager().registerEvents(timeTracker, this);
        MyCoinsVersionChecker.checkVersion(pluginName, currentVersion, logger, checkForUpdates, autoDownloadUpdates);
        logger.pluginStart();
    }

    private void createLocalConfig() {
        File localFile = new File(getDataFolder(), "local.yml");
        if (!localFile.exists()) {
            localFile.getParentFile().mkdirs();
            saveResource("local.yml", false);
        }

        localConfig = new YamlConfiguration();
        try {
            localConfig.load(localFile);
        } catch (IOException | InvalidConfigurationException e) {
            logger.err("Wystąpił błąd podczas ładowania pliku local.yml: " + e.getMessage());
        }
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