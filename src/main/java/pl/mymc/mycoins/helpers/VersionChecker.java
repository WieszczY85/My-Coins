package pl.mymc.mycoins.helpers;

import com.eternalcode.gitcheck.GitCheck;
import com.eternalcode.gitcheck.GitCheckResult;
import com.eternalcode.gitcheck.git.GitRelease;
import com.eternalcode.gitcheck.git.GitRepository;
import com.eternalcode.gitcheck.git.GitTag;

public class VersionChecker {
    public static void checkVersion(String currentVersion, MyCoinsLogger logger) {
        GitCheck gitCheck = new GitCheck();
        GitRepository repository = GitRepository.of("WieszczY85", "My-Coins");

        GitCheckResult result = gitCheck.checkRelease(repository, GitTag.of(currentVersion));

        if (!result.isUpToDate()) {
            GitRelease release = result.getLatestRelease();
            GitTag tag = release.getTag();

            logger.info("A new version is available: " + tag.getTag());
            logger.info("See release page: " + release.getPageUrl());
            logger.info("Release date: " + release.getPublishedAt());
        }
    }
}
