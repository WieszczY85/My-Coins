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
import java.sql.Statement;

public final class My_Coin extends JavaPlugin {
    private MyCoinsLogger logger;
    private static Economy econ = null;
    private static Permission perms = null;
    private MySQLDatabaseHandler dbHandler;
    private PlayerTimeTracker timeTracker;

    @Override
    public void onLoad() {
        String fullName = getDescription().getFullName();
        String pluginName = getDescription().getRawName();
        String serverVersion = getServer().getBukkitVersion();
        MyCoinsLogger logger = new MyCoinsLogger(fullName, serverVersion, pluginName);

        // Utwórz nowy manager zależności i załaduj zależności
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
        saveDefaultConfig();
        String fullName = getDescription().getFullName();
        String pluginName = getDescription().getRawName();
        String serverVersion = getServer().getBukkitVersion();

        FileConfiguration config = getConfig();
        boolean checkForUpdates = config.getBoolean("checkForUpdates");
        boolean autoDownloadUpdates = config.getBoolean("autoDownloadUpdates");

        String currentVersion = getDescription().getVersion();

        MyCoinsLogger logger = new MyCoinsLogger(fullName, serverVersion, pluginName);

        if (!setupEconomy() ) {
            logger.err("Wyłączono, ponieważ nie znaleziono zależności Vault");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        setupPermissions();

        dbHandler = new MySQLDatabaseHandler(getConfig());
        try {
            dbHandler.openConnection();
            logger.success("Połączono z bazą danych!");
        } catch (SQLException | ClassNotFoundException e) {
            logger.err("Nie udało się połączyć z bazą danych!" + e.getMessage());
        }
        try {
            Statement statement = dbHandler.getConnection().createStatement();
            logger.info("Sprawdzanie tableli " + pluginName + " w bazie danych");
            String createTable = "CREATE TABLE IF NOT EXISTS `My-Coins` (" +
                    "`id` int(11) NOT NULL AUTO_INCREMENT," +
                    "`player` varchar(255) NOT NULL," +
                    "`uuid` varchar(255) NOT NULL," +
                    "`data` date NOT NULL," +
                    "`joinTime` time NOT NULL," +
                    "`quitTime` time NOT NULL," +
                    "`totalTime` time NOT NULL," +
                    "PRIMARY KEY (`id`)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;";

            statement.execute(createTable);
            logger.info("Zakończono operacje na tabeli");
        } catch (SQLException e) {
            logger.err("Nie udało się utworzyć tabeli w bazie danych!" + e.getMessage());
        }
        timeTracker = new PlayerTimeTracker(dbHandler, logger);
        getServer().getPluginManager().registerEvents(timeTracker, this);
        logger.pluginStart();
        logger.checkServerType();
        MyCoinsVersionChecker.checkVersion(pluginName, currentVersion, logger, checkForUpdates, autoDownloadUpdates);
    }

    private boolean setupEconomy() {
        if (Bukkit.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        final RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }


    private boolean setupPermissions() {
        RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
        perms = rsp.getProvider();
        return perms != null;
    }

    public static Economy getEconomy() {
        return econ;
    }
    public static Permission getPermissions() {
        return perms;
    }

    @Override
    public void onDisable() {
        timeTracker = new PlayerTimeTracker(dbHandler, logger);
        getServer().getPluginManager().registerEvents(timeTracker, this);
        for (Player player : Bukkit.getOnlinePlayers()) {
            timeTracker.onPlayerQuit(new PlayerQuitEvent(player, ""));
        }
        try {
            if (dbHandler.getConnection() != null && !dbHandler.getConnection().isClosed()) {
                dbHandler.getConnection().close();
                logger.warning("Połączenie z bazą danych zamknięte!");
            }
        } catch (SQLException e) {
            logger.err("Nie udało się zamknąć połączenia z bazą danych!" + e.getMessage());
        }
        logger.err(getDescription().getName() +" Disabled Version "+ getDescription().getVersion());
    }
}
