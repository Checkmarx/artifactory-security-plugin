package com.checkmarx.plugins.artifactory.configuration;

public enum ArtifactProperty {
  ISSUE_URL("scs.issue.url"),
  ISSUE_VULNERABILITIES("scs.issue.vulnerabilities"),
  ISSUE_VULNERABILITIES_FORCE_DOWNLOAD("scs.issue.vulnerabilities.forceDownload"),
  ISSUE_VULNERABILITIES_FORCE_DOWNLOAD_INFO("scs.issue.vulnerabilities.forceDownload.info"),
  ISSUE_LICENSES("scs.issue.licenses"),
  ISSUE_LICENSES_FORCE_DOWNLOAD("scs.issue.licenses.forceDownload"),
  ISSUE_LICENSES_FORCE_DOWNLOAD_INFO("scs.issue.licenses.forceDownload.info");

  private final String propertyKey;

  ArtifactProperty(String propertyKey) {
    this.propertyKey = propertyKey;
  }

  public String propertyKey() {
    return propertyKey;
  }
}
