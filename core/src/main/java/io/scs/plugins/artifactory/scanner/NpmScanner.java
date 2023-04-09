package io.scs.plugins.artifactory.scanner;

import io.scs.plugins.artifactory.configuration.ConfigurationModule;
import io.scs.plugins.artifactory.exception.CannotScanException;
import io.scs.plugins.artifactory.exception.ScsAPIFailureException;
import io.scs.sdk.api.v1.scsClient;
import io.scs.sdk.api.v1.scsResult;
import io.scs.sdk.model.TestResult;
import org.artifactory.fs.FileLayoutInfo;
import org.artifactory.repo.RepoPath;
import org.artifactory.request.Request;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.slf4j.LoggerFactory.getLogger;

class NpmScanner implements PackageScanner {

  private static final Logger LOG = getLogger(NpmScanner.class);

  private final ConfigurationModule configurationModule;
  private final scsClient scsClient;

  NpmScanner(ConfigurationModule configurationModule, scsClient scsClient) {
    this.configurationModule = configurationModule;
    this.scsClient = scsClient;
  }

  public static Optional<PackageURLDetails> getPackageDetailsFromUrl(String repoPath) {
    Pattern pattern = Pattern.compile("^(?:.+:)?(?<packageName>.+)/-/.+-(?<packageVersion>\\d+\\.\\d+\\.\\d+.*)\\.tgz$");
    Matcher matcher = pattern.matcher(repoPath);
    if (matcher.matches()) {
      return Optional.of(new PackageURLDetails(
        matcher.group("packageName"),
        matcher.group("packageVersion")
      ));
    }
    return Optional.empty();
  }


  public TestResult scan(FileLayoutInfo fileLayoutInfo, RepoPath repoPath, Request request) {
    PackageURLDetails details = getPackageDetailsFromUrl(repoPath.toString())
      .orElseThrow(() -> new CannotScanException("Package details not provided."));

    scsResult<TestResult> result;
    try {
      result = scsClient.testNpm(
        details.name,
        details.version
      );
    } catch (Exception e) {
      if (!(e.toString().contains("Unsafe package")))
          LOG.error("error in scan npm package module npmscanner: " + e);
      throw new ScsAPIFailureException(e);
    }

    TestResult testResult = result.get().orElseThrow(() -> new ScsAPIFailureException(result));
//    testResult.packageDetailsURL = getPackageDetailsURL(details);
    return testResult;
//    return null;
  }

  public static class PackageURLDetails {
    public final String name;
    public final String version;

    public PackageURLDetails(String name, String version) {
      this.name = name;
      this.version = version;
    }
  }
}