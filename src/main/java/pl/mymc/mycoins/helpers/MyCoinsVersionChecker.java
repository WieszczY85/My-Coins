package pl.mymc.mycoins.helpers;

import com.eternalcode.gitcheck.GitCheck;
import com.eternalcode.gitcheck.GitCheckResult;
import com.eternalcode.gitcheck.git.GitRelease;
import com.eternalcode.gitcheck.git.GitRepository;
import com.eternalcode.gitcheck.git.GitTag;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class MyCoinsVersionChecker {
    public static void checkVersion(String pluginName, String currentVersion, MyCoinsLogger logger, boolean checkForUpdates, boolean autoDownloadUpdates) {
        if (!checkForUpdates) {
            return;
        }
        logger.info("Sprawdzam dostępność nowszej wersji...");
        GitCheck gitCheck = new GitCheck();
        GitRepository repository = GitRepository.of("WieszczY85", pluginName);

        GitCheckResult result = gitCheck.checkRelease(repository, GitTag.of(currentVersion));

        if (!result.isUpToDate()) {
            GitRelease release = result.getLatestRelease();
            GitTag tag = release.getTag();

            logger.warning("Znaleziono nowszą wersje " + pluginName + ": " + tag.getTag());
            //logger.info("Release date: " + release.getPublishedAt());
            if (autoDownloadUpdates) {
                File newJar = new File("plugins/" + pluginName + "-" + tag.getTag() + ".jar");
                try {
                    String jarUrl = "https://github.com/WieszczY85/" + pluginName + "/releases/download/" + tag.getTag() + "/" + pluginName + "-" + tag.getTag() + ".jar";
                    URL website = new URL(jarUrl);
                    ReadableByteChannel rbc = Channels.newChannel(website.openStream());

                    FileOutputStream fos = new FileOutputStream(newJar);
                    fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                    fos.close();
                    rbc.close();
                    File oldJar = new File("plugins/" + pluginName + "-" + currentVersion + ".jar");
                    if (oldJar.exists()) {
                        oldJar.delete();
                    }

                    logger.info("Rozpoczynam pobieranie pluginu...");
                    logger.success("Pobrano i zainstalowano najnowszą wersję, która zostanie użyta po następnym restarcie serwera.");
                } catch (IOException e) {
                    logger.err("Nie udało się pobrać nowej wersji: " + e.getMessage());
                    //newJar.delete();
                }
            }else{
                logger.warning("Możesz pobrać najnowszą wersję z: " + release.getPageUrl());
            }
        }else{
            logger.info("Posiadasz już najnowszą wersję " + pluginName + ": " + currentVersion);
        }
    }
}
