package io.scs.plugins.artifactory.configuration;

public enum PluginConfiguration implements Configuration {
  // general settings
  API_URL("scs.api.url", "https://api.dusti.co/v1/sca/packages"),
  API_TOKEN("scs.api.token", ""),
  API_ORGANIZATION("scs.api.organization", ""),
  API_SSL_CERTIFICATE_PATH("scs.api.sslCertificatePath", ""),
  API_TRUST_ALL_CERTIFICATES("scs.api.trustAllCertificates", "false"),
  API_TIMEOUT("scs.api.timeout", "60000"),

  HTTP_PROXY_HOST("scs.http.proxyHost", ""),
  HTTP_PROXY_PORT("scs.http.proxyPort", "80"),

  // scanner module
  SCANNER_BLOCK_ON_API_FAILURE("scs.scanner.block-on-api-failure", "false"),
  SCANNER_VULNERABILITY_THRESHOLD("scs.scanner.vulnerability.threshold", "low"),
  SCANNER_LICENSE_THRESHOLD("scs.scanner.license.threshold", "low"),
  SCANNER_PACKAGE_TYPE_MAVEN("scs.scanner.packageType.maven", "true"),
  SCANNER_PACKAGE_TYPE_NPM("scs.scanner.packageType.npm", "true"),
  SCANNER_PACKAGE_TYPE_PYPI("scs.scanner.packageType.pypi", "false");

//  SCANNER_PACKAGE_TYPE_NUGET("scs.scanner.packageType.nugget", "false");

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
