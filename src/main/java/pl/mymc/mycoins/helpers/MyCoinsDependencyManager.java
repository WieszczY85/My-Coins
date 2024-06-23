package pl.mymc.mycoins.helpers;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;

public class MyCoinsDependencyManager {
    private final URLClassLoader classLoader;
    private final MyCoinsLogger logger;

    public MyCoinsDependencyManager(URLClassLoader classLoader, MyCoinsLogger logger) {
        this.classLoader = classLoader;
        this.logger = logger;
    }

    private String calculateChecksum(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] fileBytes = Files.readAllBytes(path);
        byte[] checksumBytes = digest.digest(fileBytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : checksumBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public void loadDependency(String url, Path targetPath) throws Exception {
        // Utwórz katalog, jeśli nie istnieje
        Files.createDirectories(targetPath.getParent());

        if (Files.exists(targetPath)) {
            logger.success("Plik już istnieje, nie ma konieczności pobierania: " + targetPath);
            return;
        }
        // Pobierz zależność
        try (InputStream in = new URL(url).openStream()) {
            Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
        // Załaduj zależność
        Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        method.setAccessible(true);
        method.invoke(classLoader, targetPath.toUri().toURL());

        logger.success("Pobrano zależność z " + url);
    }


    public void loadMariaDb() throws Exception {
        loadDependency("https://repo1.maven.org/maven2/org/mariadb/jdbc/mariadb-java-client/3.4.0/mariadb-java-client-3.4.0.jar", Paths.get("plugins/My-Coins/libs/mariadb-java-client-3.4.0.jar"));
        logger.success("Pomyślnie załadowano MariaDB!");
    }
    public void loadMysql() throws Exception {
        loadDependency("https://repo1.maven.org/maven2/mysql/mysql-connector-java/8.0.23/mysql-connector-java-8.0.23.jar", Paths.get("plugins/My-Coins/libs/mysql-connector-java-8.0.23.jar"));
        logger.success("Pomyślnie załadowano MySQL-Connector!");
    }
    public void loadMiniMessage() throws Exception {
        loadDependency("https://repo1.maven.org/maven2/net/kyori/adventure-text-minimessage/4.17.0/adventure-text-minimessage-4.17.0.jar", Paths.get("plugins/My-Coins/libs/adventure-text-minimessage-4.17.0.jar"));
        logger.success("Pomyślnie załadowano MiniMessage!");
    }
    public void loadAdventureApi() throws Exception {
        loadDependency("https://repo1.maven.org/maven2/net/kyori/adventure-api/4.17.0/adventure-api-4.17.0.jar", Paths.get("plugins/My-Coins/libs/adventure-api-4.17.0.jar"));
        logger.success("Pomyślnie załadowano AdventureAPI!");
    }
    public void loadGSON() throws Exception {
        loadDependency("https://repo1.maven.org/maven2/net/kyori/adventure-text-serializer-gson/4.17.0/adventure-text-serializer-gson-4.17.0.jar", Paths.get("plugins/My-Coins/libs/adventure-text-serializer-gson-4.17.0.jar"));
        logger.success("Pomyślnie załadowano Adventure-GSON!");
    }
    public void loadLegacy() throws Exception {
        loadDependency("https://repo1.maven.org/maven2/net/kyori/adventure-text-serializer-legacy/4.17.0/adventure-text-serializer-legacy-4.17.0.jar", Paths.get("plugins/My-Coins/libs/adventure-text-serializer-legacy-4.17.0.jar"));
        logger.success("Pomyślnie załadowano Adventure-Legacy!");
    }
    public void loadPlain() throws Exception {
        loadDependency("https://repo1.maven.org/maven2/net/kyori/adventure-text-serializer-plain/4.17.0/adventure-text-serializer-plain-4.17.0.jar", Paths.get("plugins/My-Coins/libs/adventure-text-serializer-plain-4.17.0.jar"));
        logger.success("Pomyślnie załadowano Adventure-Plain!");
    }
}

