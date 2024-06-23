package pl.mymc.mycoins;

import pl.mymc.mycoins.databases.MySQLDatabaseHandler;
import pl.mymc.mycoins.helpers.MyCoinsDependencyManager;
import pl.mymc.mycoins.helpers.MyCoinsLogger;
import pl.mymc.mycoins.helpers.VersionChecker;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.configuration.file.FileConfiguration;

import java.net.URLClassLoader;
import java.sql.SQLException;

public final class My_Coin extends JavaPlugin {
    public MyCoinsLogger logger;
    private static Economy econ = null;
    private static Permission perms = null;
    private MySQLDatabaseHandler dbHandler;

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
        String currentVersion = config.getString("version");

        MyCoinsLogger logger = new MyCoinsLogger(fullName, serverVersion, pluginName);
        VersionChecker.checkVersion(currentVersion, logger);
        logger.info("Current version: " + currentVersion);

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
            logger.err("Nie udało się połączyć z bazą danych!");
            e.printStackTrace();
        }
        logger.pluginStart();
        logger.checkServerType();
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
        logger.err(getDescription().getName() +" Disabled Version "+ getDescription().getVersion());
        try {
            if (dbHandler.getConnection() != null && !dbHandler.getConnection().isClosed()) {
                dbHandler.getConnection().close();
                logger.warning("Połączenie z bazą danych zamknięte!");
            }
        } catch (SQLException e) {
            logger.err("Nie udało się zamknąć połączenia z bazą danych!");
            e.printStackTrace();
        }
    }
}
