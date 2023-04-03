package io.scs.plugins.artifactory.scanner;

import io.scs.plugins.artifactory.configuration.ConfigurationModule;
import io.scs.plugins.artifactory.exception.CannotScanException;
import io.scs.plugins.artifactory.exception.ScsAPIFailureException;
import io.scs.sdk.api.v1.scsResult;
import io.scs.sdk.model.TestResult;
import org.artifactory.fs.FileLayoutInfo;
import org.artifactory.repo.RepoPath;
import org.artifactory.request.Request;
import org.slf4j.Logger;

import java.net.URLEncoder;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.*;
import static org.slf4j.LoggerFactory.getLogger;

class MavenScanner implements PackageScanner {

  private static final Logger LOG = getLogger(MavenScanner.class);

  private final ConfigurationModule configurationModule;
  private final io.scs.sdk.api.v1.scsClient scsClient;

  MavenScanner(ConfigurationModule configurationModule, io.scs.sdk.api.v1.scsClient scsClient) {
    this.configurationModule = configurationModule;
    this.scsClient = scsClient;
  }

  public TestResult scan(FileLayoutInfo fileLayoutInfo, RepoPath repoPath, Request request) {
    String groupID = Optional.ofNullable(fileLayoutInfo.getOrganization())
      .orElseThrow(() -> new CannotScanException("Group ID not provided."));
    String artifactID = Optional.ofNullable(fileLayoutInfo.getModule())
      .orElseThrow(() -> new CannotScanException("Artifact ID not provided."));
    String artifactVersion = Optional.ofNullable(fileLayoutInfo.getBaseRevision())
      .orElseThrow(() -> new CannotScanException("Artifact Version not provided."));

    scsResult<TestResult> result;
    try {
      result = scsClient.testMaven(
        groupID,
        artifactID,
        artifactVersion
      );
    } catch (Exception e) {
      throw new ScsAPIFailureException(e);
    }

    TestResult testResult = result.get().orElseThrow(() -> new ScsAPIFailureException(result));
//    testResult.packageDetailsURL = getArtifactDetailsURL(groupID, artifactID, artifactVersion);
    return testResult;
  }
}
