package com.checkmarx.plugins.artifactory;

import com.checkmarx.plugins.artifactory.audit.AuditModule;
import com.checkmarx.plugins.artifactory.configuration.ArtifactProperty;
import com.checkmarx.plugins.artifactory.configuration.ConfigurationModule;
import com.checkmarx.plugins.artifactory.exception.CannotScanException;
import com.checkmarx.plugins.artifactory.exception.ScsAPIFailureException;
import com.checkmarx.plugins.artifactory.exception.ScsRuntimeException;
import com.checkmarx.plugins.artifactory.scanner.ScannerModule;
import com.checkmarx.sdk.ScsConfig;
import com.checkmarx.sdk.api.v1.scsClient;
import com.checkmarx.sdk.api.v1.scsResult;
import com.checkmarx.sdk.model.NotificationSettings;
import org.artifactory.exception.CancelException;
import org.artifactory.fs.ItemInfo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.Repositories;
import org.artifactory.request.Request;
import org.artifactory.security.User;
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

public class ScSPlugin {

  private static final Logger LOG = LoggerFactory.getLogger(ScSPlugin.class);
  private static final String API_USER_AGENT = "scs-artifactory-plugin/";

  private ConfigurationModule configurationModule;
  private AuditModule auditModule;
  private ScannerModule scannerModule;

  ScSPlugin() {
  }

  public ScSPlugin(@Nonnull Repositories repositories, File pluginsDirectory) {
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
      final scsClient scsClient = createScsClient(configurationModule, pluginVersion);
      LOG.debug("finished creating scs client");
      LOG.debug("creating audit module");
      auditModule = new AuditModule();
      LOG.debug("fnished creating audit module");
      LOG.debug("creating scanner module");
      scannerModule = new ScannerModule(configurationModule, repositories, scsClient);
      LOG.debug("finished creating scanner module");
      LOG.info("Plugin version: {}", pluginVersion);
    } catch (Exception ex) {
      throw new ScsRuntimeException("scs plugin could not be initialized!", ex);
    }
  }

  /**
   * Logs update event for following artifact properties:
   * <ul>
   * <li>{@link ArtifactProperty#ISSUE_LICENSES_FORCE_DOWNLOAD}</li>
   * <li>{@link ArtifactProperty#ISSUE_LICENSES_FORCE_DOWNLOAD_INFO}</li>
   * <li>{@link ArtifactProperty#ISSUE_VULNERABILITIES_FORCE_DOWNLOAD}</li>
   * <li>{@link ArtifactProperty#ISSUE_VULNERABILITIES_FORCE_DOWNLOAD_INFO}</li>
   * </ul>
   * <p>
   * Extension point: {@code storage.afterPropertyCreate}.
   */
  public void handleAfterPropertyCreateEvent(User user, ItemInfo itemInfo, String propertyName, String[] propertyValues) {
    LOG.debug("Handle 'afterPropertyCreate' event for: {}", itemInfo);
    auditModule.logPropertyUpdate(user, itemInfo, propertyName, propertyValues);
  }

  /**
   * Scans an artifact for issues (vulnerability or license).
   * <p>
   * Extension point: {@code download.beforeDownload}.
   */
  public void handleBeforeDownloadEvent(RepoPath repoPath, Request request) {
    LOG.debug("Handle 'beforeDownload' event for: {}", repoPath);
    try {
      scannerModule.scanArtifact(repoPath,request);
    } catch (CannotScanException e) {
      LOG.debug("Artifact cannot be scanned. {} {}", e.getMessage(), repoPath);
    } catch (ScsAPIFailureException e) {
      final String blockOnApiFailurePropertyKey = SCANNER_BLOCK_ON_API_FAILURE.propertyKey();
      final String blockOnApiFailure = configurationModule.getPropertyOrDefault(SCANNER_BLOCK_ON_API_FAILURE);
      final String causeMessage = Optional.ofNullable(e.getCause())
        .map(Throwable::getMessage)
        .map(m -> e.getMessage() + " " + m)
        .orElseGet(e::getMessage);

      String message = format("Artifact scan failed. %s %s", causeMessage, repoPath);
      if ("true".equals(blockOnApiFailure)) {
        LOG.debug("Blocking download. Plugin Property \"{}\" is \"true\". {}", blockOnApiFailurePropertyKey, repoPath);
        throw new CancelException(message, 500);
      }
      LOG.debug(message);

    }
  }

  private void validateConfiguration() {
    try {
      configurationModule.validate();
    } catch (Exception ex) {
      throw new ScsRuntimeException("scs Plugin Configuration is not valid!", ex);
    }

    LOG.debug("scs Plugin Configuration:");
    configurationModule.getPropertyEntries().stream()
      .filter(entry -> !API_TOKEN.propertyKey().equals(entry.getKey()))
      .map(entry -> entry.getKey() + "=" + entry.getValue())
      .sorted()
      .forEach(LOG::debug);
  }

  private scsClient createScsClient(@Nonnull ConfigurationModule configurationModule, String pluginVersion) throws Exception {
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

    var config = ScsConfig.newBuilder()
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

    final scsClient scsClient = new scsClient(config);
    LOG.info("created scsClient");
    LOG.info("got configuration modules property or default");
//    LOG.info("getting notification settings");
//    var res = scsClient.getNotificationSettings(org);
//    LOG.info("got notification settings");
//    handleResponse(res);
    LOG.info("handle Response finished");

    return scsClient;
  }

  void handleResponse(scsResult<NotificationSettings> res) {
    if (res.isSuccessful()) {
      LOG.info("Scs token check successful - response status code {}", res.statusCode);
    } else {
      String info = "";
      if (null != res.response) {
        HttpRequest request = res.response.request();
        info += "\nRequest URI: " + request.uri();
        info += "\nRequest Headers: " + sanitizeHeaders(request);
        info += "\nResponse Status: " + res.response.statusCode();
        info += "\nResponse Body: " + res.response.body();
      }
      LOG.warn("scs token check unsuccessful - response status code {}{}", res.statusCode, info);
      if (res.statusCode == 401) {
        throw new ScsRuntimeException(format("%s is not valid.%s", API_TOKEN.propertyKey(), info));
      } else {
        throw new ScsRuntimeException(format("%s could not be verified.%s", API_TOKEN.propertyKey(), info));
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
