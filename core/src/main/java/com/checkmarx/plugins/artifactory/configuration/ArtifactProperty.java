package com.checkmarx.plugins.artifactory.configuration;

public enum ArtifactProperty {
  ISSUE_URL("checkmarx.issue.url"),
  ISSUE_VULNERABILITIES("checkmarx.issue.vulnerabilities"),
  ISSUE_VULNERABILITIES_FORCE_DOWNLOAD("checkmarx.issue.vulnerabilities.forceDownload"),
  ISSUE_VULNERABILITIES_FORCE_DOWNLOAD_INFO("checkmarx.issue.vulnerabilities.forceDownload.info"),
  ISSUE_LICENSES("checkmarx.issue.licenses"),
  ISSUE_LICENSES_FORCE_DOWNLOAD("checkmarx.issue.licenses.forceDownload"),
  ISSUE_LICENSES_FORCE_DOWNLOAD_INFO("checkmarx.issue.licenses.forceDownload.info");

  private final String propertyKey;

  ArtifactProperty(String propertyKey) {
    this.propertyKey = propertyKey;
  }

  public String propertyKey() {
    return propertyKey;
  }
}
