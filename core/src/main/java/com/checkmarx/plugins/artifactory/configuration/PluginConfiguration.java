package com.checkmarx.plugins.artifactory.configuration;

public enum PluginConfiguration implements Configuration {
  // general settings
  API_URL("checkmarx.api.url", "https://api.dusti.co/v1/packages/"),
  API_TOKEN("checkmarx.api.token", ""),
  API_SSL_CERTIFICATE_PATH("checkmarx.api.sslCertificatePath", ""),
  API_TRUST_ALL_CERTIFICATES("checkmarx.api.trustAllCertificates", "false"),
  API_TIMEOUT("checkmarx.api.timeout", "60000"),

  HTTP_PROXY_HOST("checkmarx.http.proxyHost", ""),
  HTTP_PROXY_PORT("checkmarx.http.proxyPort", "80"),

  // scanner module
  SCANNER_PACKAGE_TYPE_MAVEN("checkmarx.scanner.packageType.maven", "true"),
  SCANNER_PACKAGE_TYPE_NPM("checkmarx.scanner.packageType.npm", "true"),
  SCANNER_PACKAGE_TYPE_PYPI("checkmarx.scanner.packageType.pypi", "false");

  private final String propertyKey;
  private final String defaultValue;

  PluginConfiguration(String propertyKey, String defaultValue) {
    this.propertyKey = propertyKey;
    this.defaultValue = defaultValue;
  }

  @Override
  public String propertyKey() {
    return propertyKey;
  }

  @Override
  public String defaultValue() {
    return defaultValue;
  }
}
