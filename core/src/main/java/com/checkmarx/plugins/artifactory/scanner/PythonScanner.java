package com.checkmarx.plugins.artifactory.scanner;

import com.checkmarx.plugins.artifactory.configuration.ConfigurationModule;
import com.checkmarx.sdk.api.v1.CheckmarxClient;
import com.checkmarx.sdk.api.v1.CheckmarxResult;
import com.checkmarx.sdk.model.TestResult;
import com.checkmarx.plugins.artifactory.exception.CannotScanException;
import com.checkmarx.plugins.artifactory.exception.CheckmarxAPIFailureException;
import org.artifactory.fs.FileLayoutInfo;
import org.artifactory.repo.RepoPath;
import org.artifactory.request.Request;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.slf4j.LoggerFactory.getLogger;

class PythonScanner implements PackageScanner {

  private static final Logger LOG = getLogger(PythonScanner.class);

  private final ConfigurationModule configurationModule;
  private final CheckmarxClient checkmarxClient;

  PythonScanner(ConfigurationModule configurationModule, CheckmarxClient checkmarxClient) {
    this.configurationModule = configurationModule;
    this.checkmarxClient = checkmarxClient;
  }

  public static Optional<ModuleURLDetails> getModuleDetailsFromFileLayoutInfo(FileLayoutInfo fileLayoutInfo) {
    String module = fileLayoutInfo.getModule();
    String baseRevision = fileLayoutInfo.getBaseRevision();
    if (module == null || baseRevision == null) {
      return Optional.empty();
    }
    return Optional.of(new ModuleURLDetails(
      module,
      baseRevision
    ));
  }

  public static Optional<ModuleURLDetails> getModuleDetailsFromUrl(String repoPath) {
    Pattern pattern = Pattern.compile("^.+:.+/.+/.+/(?<packageName>.+)-(?<packageVersion>\\d+(?:\\.[A-Za-z0-9]+)*).*\\.(?:whl|egg|zip|tar\\.gz)$");
    Matcher matcher = pattern.matcher(repoPath);
    if (matcher.matches()) {
      return Optional.of(new ModuleURLDetails(
        matcher.group("packageName"),
        matcher.group("packageVersion")
      ));
    }
    return Optional.empty();
  }


  public TestResult scan(FileLayoutInfo fileLayoutInfo, RepoPath repoPath, Request request) {
    ModuleURLDetails details = getModuleDetailsFromFileLayoutInfo(fileLayoutInfo)
      .orElseGet(() -> getModuleDetailsFromUrl(repoPath.toString())
        .orElseThrow(() -> new CannotScanException("Module details not provided.")));

    CheckmarxResult<TestResult> result;
    try {
      result = checkmarxClient.testPip(
        details.name,
        details.version
      );
    } catch (Exception e) {
      if (!(e.toString().contains("Unsafe package")))
          LOG.error("error in scan pypi package module pypiscanner: " + e);
      throw new CheckmarxAPIFailureException(e);
    }

    TestResult testResult = result.get().orElseThrow(() -> new CheckmarxAPIFailureException(result));
    return testResult;
  }

  public static class ModuleURLDetails {
    public final String name;
    public final String version;

    public ModuleURLDetails(String name, String version) {
      this.name = name;
      this.version = version;
    }
  }
}
