package io.scs.plugins.artifactory.scanner;

import io.scs.plugins.artifactory.configuration.ArtifactProperty;
import io.scs.plugins.artifactory.configuration.ConfigurationModule;
import io.scs.plugins.artifactory.exception.CannotScanException;
import io.scs.sdk.api.v1.scsClient;
import io.scs.sdk.model.Issue;
import io.scs.sdk.model.Severity;
import io.scs.sdk.model.TestResult;
import org.artifactory.fs.FileLayoutInfo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.Repositories;
import org.artifactory.request.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

import static io.scs.plugins.artifactory.configuration.PluginConfiguration.*;
import static io.scs.sdk.util.Predicates.distinctByKey;
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

  public ScannerModule(@Nonnull ConfigurationModule configurationModule, @Nonnull Repositories repositories, @Nonnull scsClient scsClient) {
    this.configurationModule = requireNonNull(configurationModule);
    this.repositories = requireNonNull(repositories);

    mavenScanner = new MavenScanner(configurationModule, scsClient);
    npmScanner = new NpmScanner(configurationModule, scsClient);
    pythonScanner = new PythonScanner(configurationModule, scsClient);
    nuggetScanner = new NuggetScanner(configurationModule, scsClient);

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
      if (configurationModule.getPropertyOrDefault(SCANNER_PACKAGE_TYPE_MAVEN).equals("true")) {
        return mavenScanner;
      }
      throw new CannotScanException(format("Plugin Property \"%s\" is not \"true\".", SCANNER_PACKAGE_TYPE_MAVEN.propertyKey()));
    }

    if (path.endsWith(".tgz")) {
      if (configurationModule.getPropertyOrDefault(SCANNER_PACKAGE_TYPE_NPM).equals("true")) {
        return npmScanner;
      }
      throw new CannotScanException(format("Plugin Property \"%s\" is not \"true\".", SCANNER_PACKAGE_TYPE_NPM.propertyKey()));
    }

    if (path.endsWith(".whl") || path.endsWith(".tar.gz") || path.endsWith(".zip") || path.endsWith(".egg")) {
      if (configurationModule.getPropertyOrDefault(SCANNER_PACKAGE_TYPE_PYPI).equals("true")) {
        return pythonScanner;
      }
      throw new CannotScanException(format("Plugin Property \"%s\" is not \"true\".", SCANNER_PACKAGE_TYPE_PYPI.propertyKey()));
    }

    if (path.endsWith(".nupkg")) {
      return nuggetScanner;
    }

    throw new CannotScanException("Artifact is not supported.");
  }

//  protected void updateProperties(RepoPath repoPath, TestResult testResult) {
//    repositories.setProperty(repoPath, ISSUE_VULNERABILITIES.propertyKey(), getIssuesAsFormattedString(testResult.issues.vulnerabilities));
//    repositories.setProperty(repoPath, ISSUE_LICENSES.propertyKey(), getIssuesAsFormattedString(testResult.issues.licenses));
//    repositories.setProperty(repoPath, ISSUE_URL.propertyKey(), testResult.packageDetailsURL);
//
//    setDefaultArtifactProperty(repoPath, ISSUE_VULNERABILITIES_FORCE_DOWNLOAD, "false");
//    setDefaultArtifactProperty(repoPath, ISSUE_VULNERABILITIES_FORCE_DOWNLOAD_INFO, "");
//    setDefaultArtifactProperty(repoPath, ISSUE_LICENSES_FORCE_DOWNLOAD, "false");
//    setDefaultArtifactProperty(repoPath, ISSUE_LICENSES_FORCE_DOWNLOAD_INFO, "");
//  }

  private void setDefaultArtifactProperty(RepoPath repoPath, ArtifactProperty property, String value) {
    String key = property.propertyKey();
    if (!repositories.hasProperty(repoPath, key)) {
      repositories.setProperty(repoPath, key, value);
    }
  }

  private String getIssuesAsFormattedString(@Nonnull List<? extends Issue> issues) {
    long countCriticalSeverities = issues.stream()
      .filter(issue -> issue.severity == Severity.CRITICAL)
      .filter(distinctByKey(issue -> issue.id))
      .count();
    long countHighSeverities = issues.stream()
      .filter(issue -> issue.severity == Severity.HIGH)
      .filter(distinctByKey(issue -> issue.id))
      .count();
    long countMediumSeverities = issues.stream()
      .filter(issue -> issue.severity == Severity.MEDIUM)
      .filter(distinctByKey(issue -> issue.id))
      .count();
    long countLowSeverities = issues.stream()
      .filter(issue -> issue.severity == Severity.LOW)
      .filter(distinctByKey(issue -> issue.id))
      .count();

    return format("%d critical, %d high, %d medium, %d low", countCriticalSeverities, countHighSeverities, countMediumSeverities, countLowSeverities);
  }

//  protected void validateVulnerabilityIssues(TestResult testResult, RepoPath repoPath) {
//    final String vulnerabilitiesForceDownloadProperty = ISSUE_VULNERABILITIES_FORCE_DOWNLOAD.propertyKey();
//    final String vulnerabilitiesForceDownload = repositories.getProperty(repoPath, vulnerabilitiesForceDownloadProperty);
//    final boolean forceDownload = "true".equalsIgnoreCase(vulnerabilitiesForceDownload);
//    if (forceDownload) {
//      LOG.debug("Allowing download. Artifact Property \"{}\" is \"true\". {}", vulnerabilitiesForceDownloadProperty, repoPath);
//      return;
//    }
//
//    Severity vulnerabilityThreshold = Severity.of(configurationModule.getPropertyOrDefault(PluginConfiguration.SCANNER_VULNERABILITY_THRESHOLD));
//    if (vulnerabilityThreshold == Severity.LOW) {
//      if (!testResult.issues.vulnerabilities.isEmpty()) {
//        throw new CancelException(format("Artifact has vulnerabilities. %s", repoPath), 403);
//      }
//    } else if (vulnerabilityThreshold == Severity.MEDIUM) {
//      long count = testResult.issues.vulnerabilities.stream()
//        .filter(vulnerability -> vulnerability.severity == Severity.MEDIUM || vulnerability.severity == Severity.HIGH || vulnerability.severity == Severity.CRITICAL)
//        .count();
//      if (count > 0) {
//        throw new CancelException(format("Artifact has vulnerabilities with medium, high or critical severity. %s", repoPath), 403);
//      }
//    } else if (vulnerabilityThreshold == Severity.HIGH) {
//      long count = testResult.issues.vulnerabilities.stream()
//        .filter(vulnerability -> vulnerability.severity == Severity.HIGH || vulnerability.severity == Severity.CRITICAL)
//        .count();
//      if (count > 0) {
//        throw new CancelException(format("Artifact has vulnerabilities with high or critical severity. %s", repoPath), 403);
//      }
//    } else if (vulnerabilityThreshold == Severity.CRITICAL) {
//      long count = testResult.issues.vulnerabilities.stream()
//        .filter(vulnerability -> vulnerability.severity == Severity.CRITICAL)
//        .count();
//      if (count > 0) {
//        throw new CancelException(format("Artifact has vulnerabilities with critical severity. %s", repoPath), 403);
//      }
//    }
//  }

}