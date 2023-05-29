package com.checkmarx.plugins.artifactory;

import com.checkmarx.plugins.artifactory.audit.AuditModule;
import com.checkmarx.plugins.artifactory.configuration.ConfigurationModule;
import com.checkmarx.plugins.artifactory.exception.CheckmarxRuntimeException;
import com.checkmarx.plugins.artifactory.scanner.ScannerModule;
import com.checkmarx.sdk.CheckmarxConfig;
import com.checkmarx.sdk.api.v1.CheckmarxClient;
import com.checkmarx.sdk.api.v1.CheckmarxResult;
import com.checkmarx.sdk.model.NotificationSettings;
import org.artifactory.repo.Repositories;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Optional;
import java.util.Properties;

import static com.checkmarx.plugins.artifactory.configuration.PluginConfiguration.*;
import static java.lang.String.format;

public class CheckmarxPlugin {

  private static final Logger LOG = LoggerFactory.getLogger(CheckmarxPlugin.class);
  private static final String API_USER_AGENT = "checkmarx-artifactory-plugin/";

  private ConfigurationModule configurationModule;
  private AuditModule auditModule;
  private ScannerModule scannerModule;

  CheckmarxPlugin() {
  }

  public CheckmarxPlugin(@Nonnull Repositories repositories, File pluginsDirectory) {
    try {
      LOG.info("Loading and validating plugin properties...");
      Properties properties = PropertyLoader.loadProperties(pluginsDirectory);
      String pluginVersion = PropertyLoader.loadPluginVersion(pluginsDirectory);
      configurationModule = new ConfigurationModule(properties);
      validateConfiguration();

      LOG.info("Creating api client and modules...");
      LOG.info("BaseURL:" + configurationModule.getPropertyOrDefault(API_URL));
      String token = configurationModule.getPropertyOrDefault(API_TOKEN);
      if (null != token && token.length() > 4) {
        token = token.substring(0, 4) + "...";
      } else {
        token = "no token configured";
      }
      LOG.debug("Token:" + token);
      final CheckmarxClient checkmarxClient = createCheckmarxClient(configurationModule, pluginVersion);
      LOG.debug("finished creating checkmarx client");
      LOG.debug("creating audit module");
      auditModule = new AuditModule();
      LOG.debug("fnished creating audit module");
      LOG.debug("creating scanner module");
      scannerModule = new ScannerModule(configurationModule, repositories, checkmarxClient);
      LOG.debug("finished creating scanner module");
      LOG.info("Plugin version: {}", pluginVersion);
    } catch (Exception ex) {
      throw new CheckmarxRuntimeException("checkmarx plugin could not be initialized!", ex);
    }
  }

  private void validateConfiguration() {
    try {
      configurationModule.validate();
    } catch (Exception ex) {
      throw new CheckmarxRuntimeException("checkmarx Plugin Configuration is not valid!", ex);
    }

    LOG.debug("checkmarx Plugin Configuration:");
    configurationModule.getPropertyEntries().stream()
      .filter(entry -> !API_TOKEN.propertyKey().equals(entry.getKey()))
      .map(entry -> entry.getKey() + "=" + entry.getValue())
      .sorted()
      .forEach(LOG::debug);
  }

  private CheckmarxClient createCheckmarxClient(@Nonnull ConfigurationModule configurationModule, String pluginVersion) throws Exception {
    final String token = configurationModule.getPropertyOrDefault(API_TOKEN);
    String baseUrl = configurationModule.getPropertyOrDefault(API_URL);
    boolean trustAllCertificates = false;
    String trustAllCertificatesProperty = configurationModule.getPropertyOrDefault(API_TRUST_ALL_CERTIFICATES);
    if ("true".equals(trustAllCertificatesProperty)) {
      trustAllCertificates = true;
    }

    if (!baseUrl.endsWith("/")) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("'{}' must end in /, your value is '{}'", API_URL.propertyKey(), baseUrl);
      }
      baseUrl = baseUrl + "/";
    }

    String sslCertificatePath = configurationModule.getPropertyOrDefault(API_SSL_CERTIFICATE_PATH);
    String httpProxyHost = configurationModule.getPropertyOrDefault(HTTP_PROXY_HOST);
    Integer httpProxyPort = Integer.parseInt(configurationModule.getPropertyOrDefault(HTTP_PROXY_PORT));
    Duration timeout = Duration.ofMillis(Integer.parseInt(configurationModule.getPropertyOrDefault(API_TIMEOUT)));

    var config = CheckmarxConfig.newBuilder()
      .setBaseUrl(baseUrl)
      .setToken(token)
      .setUserAgent(API_USER_AGENT + pluginVersion)
      .setTrustAllCertificates(trustAllCertificates)
      .setSslCertificatePath(sslCertificatePath)
      .setHttpProxyHost(httpProxyHost)
      .setHttpProxyPort(httpProxyPort)
      .setTimeout(timeout)
      .build();

    LOG.debug("about to log config...");
    LOG.debug("config.httpProxyHost: " + config.httpProxyHost);
    LOG.debug("config.httpProxyPort: " + config.httpProxyPort);

    final CheckmarxClient checkmarxClient = new CheckmarxClient(config);
    LOG.info("created checkmarxClient");
    LOG.info("got configuration modules property or default");
    LOG.info("handle Response finished");

    return checkmarxClient;
  }

  void handleResponse(CheckmarxResult<NotificationSettings> res) {
    if (res.isSuccessful()) {
      LOG.info("checkmarx token check successful - response status code {}", res.statusCode);
    } else {
      String info = "";
      if (null != res.response) {
        HttpRequest request = res.response.request();
        info += "\nRequest URI: " + request.uri();
        info += "\nRequest Headers: " + sanitizeHeaders(request);
        info += "\nResponse Status: " + res.response.statusCode();
        info += "\nResponse Body: " + res.response.body();
      }
      LOG.warn("checkmarx token check unsuccessful - response status code {}{}", res.statusCode, info);
      if (res.statusCode == 401) {
        throw new CheckmarxRuntimeException(format("%s is not valid.%s", API_TOKEN.propertyKey(), info));
      } else {
        throw new CheckmarxRuntimeException(format("%s could not be verified.%s", API_TOKEN.propertyKey(), info));
      }
    }
  }

  @NotNull
  static String sanitizeHeaders(HttpRequest request) {
    Optional<String> authorization = request.headers().firstValue("Authorization");
    if (authorization.isPresent()) {
      String header = authorization.get();
      if (header.contains("token") && header.length() > 10) {
        String maskedAuthHeader = header.substring(0, 10) + "...";
        return request.headers().toString().replace(header, maskedAuthHeader);
      }
    }
    return request.headers().toString();
  }
}
