package pl.mymc.mycoins.helpers;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.*;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class MyCoinsDependencyManager {
    private final MyCoinsLogger logger;
    private final RepositorySystem system;
    private final RepositorySystemSession session;

    public MyCoinsDependencyManager(MyCoinsLogger logger) {
        this.logger = logger;
        this.system = newRepositorySystem();
        this.session = newSession(system);
    }

    public void loadDependency(String coordinates, Path targetPath) throws Exception {
        Artifact artifact = new DefaultArtifact(coordinates);
        ArtifactRequest artifactRequest = new ArtifactRequest();
        artifactRequest.setArtifact(artifact);
        artifactRequest.addRepository(new RemoteRepository.Builder("central", "default", "https://repo1.maven.org/maven2/").build());

        ArtifactResult artifactResult = system.resolveArtifact(session, artifactRequest);
        artifact = artifactResult.getArtifact();

        Files.copy(artifact.getFile().toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        logger.success("Pobrano zależność z " + coordinates);
    }

    private RepositorySystem newRepositorySystem() {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);

        return locator.getService(RepositorySystem.class);
    }

    private RepositorySystemSession newSession(RepositorySystem system) {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        LocalRepository localRepo = new LocalRepository("target/local-repo");
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));

        return session;
    }

    public void loadMariaDb() throws Exception {
        loadDependency("org.mariadb.jdbc:mariadb-java-client:3.4.0", Paths.get("plugins/My-Coins/libs/mariadb-java-client-3.4.0.jar"));
        logger.success("Pomyślnie załadowano MariaDB!");
    }

    public void loadMysql() throws Exception {
        loadDependency("mysql:mysql-connector-java:8.0.23", Paths.get("plugins/My-Coins/libs/mysql-connector-java-8.0.23.jar"));
        logger.success("Pomyślnie załadowano MySQL-Connector!");
    }

    public void loadMiniMessage() throws Exception {
        loadDependency("net.kyori:adventure-text-minimessage:4.17.0", Paths.get("plugins/My-Coins/libs/adventure-text-minimessage-4.17.0.jar"));
        logger.success("Pomyślnie załadowano MiniMessage!");
    }

    public void loadAdventureApi() throws Exception {
        loadDependency("net.kyori:adventure-api:4.17.0", Paths.get("plugins/My-Coins/libs/adventure-api-4.17.0.jar"));
        logger.success("Pomyślnie załadowano AdventureAPI!");
    }

    public void loadGSON() throws Exception {
        loadDependency("net.kyori:adventure-text-serializer-gson:4.17.0", Paths.get("plugins/My-Coins/libs/adventure-text-serializer-gson-4.17.0.jar"));
        logger.success("Pomyślnie załadowano Adventure-GSON!");
    }

    public void loadLegacy() throws Exception {
        loadDependency("net.kyori:adventure-text-serializer-legacy:4.17.0", Paths.get("plugins/My-Coins/libs/adventure-text-serializer-legacy-4.17.0.jar"));
        logger.success("Pomyślnie załadowano Adventure-Legacy!");
    }

    public void loadPlain() throws Exception {
        loadDependency("net.kyori:adventure-text-serializer-plain:4.17.0", Paths.get("plugins/My-Coins/libs/adventure-text-serializer-plain-4.17.0.jar"));
        logger.success("Pomyślnie załadowano Adventure-Plain!");
    }

}

