package com.checkmarx.plugins.artifactory.scanner;

import com.checkmarx.plugins.artifactory.configuration.ArtifactProperty;
import com.checkmarx.plugins.artifactory.configuration.ConfigurationModule;
import com.checkmarx.plugins.artifactory.configuration.PluginConfiguration;
import com.checkmarx.plugins.artifactory.exception.CannotScanException;
import com.checkmarx.sdk.api.v1.CheckmarxClient;
import com.checkmarx.sdk.model.Issue;
import com.checkmarx.sdk.model.Severity;
import com.checkmarx.sdk.model.TestResult;
import com.checkmarx.sdk.util.Predicates;
import org.artifactory.fs.FileLayoutInfo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.Repositories;
import org.artifactory.request.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public class ScannerModule {

  private static final Logger LOG = LoggerFactory.getLogger(ScannerModule.class);

  private final ConfigurationModule configurationModule;
  private final Repositories repositories;
  private final MavenScanner mavenScanner;
  private final NpmScanner npmScanner;
  private final PythonScanner pythonScanner;

  private final NuggetScanner nuggetScanner;

  public ScannerModule(@Nonnull ConfigurationModule configurationModule, @Nonnull Repositories repositories, @Nonnull CheckmarxClient checkmarxClient) {
    this.configurationModule = requireNonNull(configurationModule);
    this.repositories = requireNonNull(repositories);

    mavenScanner = new MavenScanner(configurationModule, checkmarxClient);
    npmScanner = new NpmScanner(configurationModule, checkmarxClient);
    pythonScanner = new PythonScanner(configurationModule, checkmarxClient);
    nuggetScanner = new NuggetScanner(configurationModule, checkmarxClient);

  }

  public void scanArtifact(@Nonnull RepoPath repoPath, Request request) {

    String path = Optional.ofNullable(repoPath.getPath())
      .orElseThrow(() -> new CannotScanException("Path not provided."));

    PackageScanner scanner = getScannerForPackageType(path);
    FileLayoutInfo fileLayoutInfo = repositories.getLayoutInfo(repoPath);

    TestResult testResult = scanner.scan(fileLayoutInfo, repoPath,request);
//    updateProperties(repoPath, testResult);
//    validateVulnerabilityIssues(testResult, repoPath);
  }

  protected PackageScanner getScannerForPackageType(String path) {
    if (path.endsWith(".jar")) {
      if (configurationModule.getPropertyOrDefault(PluginConfiguration.SCANNER_PACKAGE_TYPE_MAVEN).equals("true")) {
        return mavenScanner;
      }
      throw new CannotScanException(String.format("Plugin Property \"%s\" is not \"true\".", PluginConfiguration.SCANNER_PACKAGE_TYPE_MAVEN.propertyKey()));
    }

    if (path.endsWith(".tgz")) {
      if (configurationModule.getPropertyOrDefault(PluginConfiguration.SCANNER_PACKAGE_TYPE_NPM).equals("true")) {
        return npmScanner;
      }
      throw new CannotScanException(String.format("Plugin Property \"%s\" is not \"true\".", PluginConfiguration.SCANNER_PACKAGE_TYPE_NPM.propertyKey()));
    }

    if (path.endsWith(".whl") || path.endsWith(".tar.gz") || path.endsWith(".zip") || path.endsWith(".egg")) {
      if (configurationModule.getPropertyOrDefault(PluginConfiguration.SCANNER_PACKAGE_TYPE_PYPI).equals("true")) {
        return pythonScanner;
      }
      throw new CannotScanException(String.format("Plugin Property \"%s\" is not \"true\".", PluginConfiguration.SCANNER_PACKAGE_TYPE_PYPI.propertyKey()));
    }

    if (path.endsWith(".nupkg")) {
      return nuggetScanner;
    }

    throw new CannotScanException("Artifact is not supported.");
  }

  private void setDefaultArtifactProperty(RepoPath repoPath, ArtifactProperty property, String value) {
    String key = property.propertyKey();
    if (!repositories.hasProperty(repoPath, key)) {
      repositories.setProperty(repoPath, key, value);
    }
  }

  private String getIssuesAsFormattedString(@Nonnull List<? extends Issue> issues) {
    long countCriticalSeverities = issues.stream()
      .filter(issue -> issue.severity == Severity.CRITICAL)
      .filter(Predicates.distinctByKey(issue -> issue.id))
      .count();
    long countHighSeverities = issues.stream()
      .filter(issue -> issue.severity == Severity.HIGH)
      .filter(Predicates.distinctByKey(issue -> issue.id))
      .count();
    long countMediumSeverities = issues.stream()
      .filter(issue -> issue.severity == Severity.MEDIUM)
      .filter(Predicates.distinctByKey(issue -> issue.id))
      .count();
    long countLowSeverities = issues.stream()
      .filter(issue -> issue.severity == Severity.LOW)
      .filter(Predicates.distinctByKey(issue -> issue.id))
      .count();

    return format("%d critical, %d high, %d medium, %d low", countCriticalSeverities, countHighSeverities, countMediumSeverities, countLowSeverities);
  }

}
