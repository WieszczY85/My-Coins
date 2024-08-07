package pl.mymc.mycoins;

import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.configuration.file.FileConfiguration;

import org.bstats.bukkit.Metrics;

import org.bukkit.plugin.java.JavaPlugin;
import pl.mymc.mycoins.databases.DatabaseHandler;
import pl.mymc.mycoins.databases.MySQLDatabaseHandler;
import pl.mymc.mycoins.databases.PostgreSQLDatabaseHandler;
import pl.mymc.mycoins.databases.SQLiteDatabaseHandler;
import pl.mymc.mycoins.myc.PlayerTimeTracker;
import pl.mymc.mycoins.helpers.MyCoinsLogger;
import pl.mymc.mycoins.helpers.MyCoinsVersionChecker;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;


public final class My_Coin extends JavaPlugin {

    //TODO:
    // * Dodać sprawdzanie czy gracz się porusza i jeśli nie to czy ma AFK
    // * Dodać obsługe AFK z EternalsCore i EssentialsX

    private final MyCoinsLogger logger;
    private static Economy econ = null;
    private static Permission perms = null;
    private DatabaseHandler dbHandler;
    private PlayerTimeTracker timeTracker;
    private final FileConfiguration config;
    private final boolean debugMode;
    private final boolean checkForUpdates;
    private final boolean autoDownloadUpdates;
    private final FileConfiguration localConfig;
    private final String dbType;
    public final String fullName;
    public final String pluginName;
    public final String serverVersion;
    public final String currentVersion;

    public My_Coin(FileConfiguration localConfig) {
        this.localConfig = localConfig;
        this.fullName = getDescription().getFullName();
        this.pluginName = getDescription().getRawName();
        this.serverVersion = getServer().getBukkitVersion();
        this.currentVersion = getDescription().getVersion();
        this.config = getConfig();
        this.debugMode = config.getBoolean("debug");
        this.checkForUpdates = config.getBoolean("checkForUpdates");
        this.autoDownloadUpdates = config.getBoolean("autoDownloadUpdates");
        this.logger = new MyCoinsLogger(fullName, serverVersion, pluginName, debugMode);
        this.dbType = config.getString("database.type");
    }

    @Override
    public void onLoad() {
        saveDefaultConfig();
        createMessageConfig();
        try {
            if (debugMode) {
                logger.success("Zakończono ładowanie wszystkie zależności!");
            }
        } catch (Exception e) {
            logger.err("Wystąpił błąd podczas ładowania zależności: " + e.getMessage());
        }
    }

    @Override
    public void onEnable() {
        if (!setupService(Economy.class)) {
            logger.err("Wyłączono, ponieważ nie znaleziono zależności Vault");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        setupService(Permission.class);

        switch (dbType) {
            case "mysql":
            case "mariadb":
                this.dbHandler = new MySQLDatabaseHandler(config, logger);
                break;
            case "sqlite":
                this.dbHandler = new SQLiteDatabaseHandler(config, logger);
                break;
            case "postgresql":
                this.dbHandler = new PostgreSQLDatabaseHandler(config, logger);
                break;
            default:
                throw new IllegalArgumentException("Nieznany typ bazy danych: " + dbType);
        }
        try {
            dbHandler.openConnectionAndCreateTable();
        } catch (SQLException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        timeTracker = new PlayerTimeTracker(dbHandler, logger, config, localConfig);
        getServer().getPluginManager().registerEvents(timeTracker, this);
        MyCoinsVersionChecker.checkVersion(pluginName, currentVersion, logger, checkForUpdates, autoDownloadUpdates);
        logger.pluginStart();
        int pluginId = 22541;
        new Metrics(this, pluginId);
    }

    private void createMessageConfig() {
        String language = config.getString("language");
        File messageFile = new File(getDataFolder(), "lang/messages_" + language + ".yml");
        if (!messageFile.exists()) {
            messageFile.getParentFile().mkdirs();
            saveResource("lang/messages_" + language + ".yml", false);
        }
        FileConfiguration messageConfig = new YamlConfiguration();
        try {
            messageConfig.load(messageFile);
        } catch (IOException | InvalidConfigurationException e) {
            logger.err("Wystąpił błąd podczas ładowania pliku messages_" + language + ".yml: " + e.getMessage());
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
            } catch (SQLException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        dbHandler.closeConnection();
        logger.err(getDescription().getName() +" Disabled Version "+ getDescription().getVersion());
    }
}