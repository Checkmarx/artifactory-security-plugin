package com.checkmarx.plugins.artifactory.scanner;

import com.checkmarx.plugins.artifactory.configuration.ConfigurationModule;
import com.checkmarx.plugins.artifactory.exception.CannotScanException;
import com.checkmarx.plugins.artifactory.exception.CheckmarxAPIFailureException;
import com.checkmarx.sdk.api.v1.CheckmarxClient;
import com.checkmarx.sdk.api.v1.CheckmarxResult;
import com.checkmarx.sdk.model.TestResult;
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
  private final CheckmarxClient checkmarxClient;

  NpmScanner(ConfigurationModule configurationModule, CheckmarxClient checkmarxClient) {
    this.configurationModule = configurationModule;
    this.checkmarxClient = checkmarxClient;
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

    CheckmarxResult<TestResult> result;
    try {
      result = checkmarxClient.testNpm(
        details.name,
        details.version
      );
    } catch (Exception e) {
      if (!(e.toString().contains("Unsafe package")))
          LOG.error("error in scan npm package module npmscanner: " + e);
      throw new CheckmarxAPIFailureException(e);
    }

    TestResult testResult = result.get().orElseThrow(() -> new CheckmarxAPIFailureException(result));
    return testResult;
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
