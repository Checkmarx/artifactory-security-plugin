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

import static org.slf4j.LoggerFactory.getLogger;

class MavenScanner implements PackageScanner {

  private static final Logger LOG = getLogger(MavenScanner.class);

  private final ConfigurationModule configurationModule;
  private final CheckmarxClient checkmarxClient;

  MavenScanner(ConfigurationModule configurationModule, CheckmarxClient checkmarxClient) {
    this.configurationModule = configurationModule;
    this.checkmarxClient = checkmarxClient;
  }

  public TestResult scan(FileLayoutInfo fileLayoutInfo, RepoPath repoPath, Request request) {
    String groupID = Optional.ofNullable(fileLayoutInfo.getOrganization())
      .orElseThrow(() -> new CannotScanException("Group ID not provided."));
    String artifactID = Optional.ofNullable(fileLayoutInfo.getModule())
      .orElseThrow(() -> new CannotScanException("Artifact ID not provided."));
    String artifactVersion = Optional.ofNullable(fileLayoutInfo.getBaseRevision())
      .orElseThrow(() -> new CannotScanException("Artifact Version not provided."));

    CheckmarxResult<TestResult> result;
    try {
      result = checkmarxClient.testMaven(
        groupID,
        artifactID,
        artifactVersion
      );
    } catch (Exception e) {
      throw new CheckmarxAPIFailureException(e);
    }

    TestResult testResult = result.get().orElseThrow(() -> new CheckmarxAPIFailureException(result));
    return testResult;
  }
}
