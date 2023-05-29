package com.checkmarx.sdk.api.v1;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.util.Optional;

import com.checkmarx.sdk.config.SSLConfiguration;
import com.checkmarx.sdk.model.NotificationSettings;
import com.checkmarx.sdk.model.TestResult;
import org.jetbrains.annotations.NotNull;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;
import com.checkmarx.sdk.CheckmarxConfig;

import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class CheckmarxClient {
  private static final Logger LOG = LoggerFactory.getLogger(CheckmarxClient.class);

  private final CheckmarxConfig config;
  private final HttpClient httpClient;

  public void isMalcious(String result) throws Exception
  {
    LOG.debug("checking if result is malicious " + result);
    JSONParser parser = new JSONParser();
    Object obj  = parser.parse(result);
    JSONArray results = (JSONArray) obj;

    for (Object r : results) {
      JSONObject element = (JSONObject) r;
      JSONArray risks = (JSONArray) (element.get("risks"));
      if (risks.size() > 0) {
        LOG.error(("Unsafe package " + element.get("type") + ":" + element.get("name") + ":" + element.get("version") + "risks: " + element.get("risks")));
        throw new Exception("Unsafe package");
      }

    }
  }
  public CheckmarxClient(CheckmarxConfig config) throws Exception {
    this.config = config;
    LOG.info("creating checkmarx client");
    var builder = HttpClient.newBuilder()
      .version(HttpClient.Version.HTTP_1_1)
      .connectTimeout(config.timeout);
    LOG.info("created http client");
    if (config.trustAllCertificates) {
      SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
      TrustManager[] trustManagers = SSLConfiguration.buildUnsafeTrustManager();
      sslContext.init(null, trustManagers, new SecureRandom());
      builder.sslContext(sslContext);
    } else if (config.sslCertificatePath != null && !config.sslCertificatePath.isEmpty()) {
      SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
      X509TrustManager trustManager = SSLConfiguration.buildCustomTrustManager(config.sslCertificatePath);
      sslContext.init(null, new TrustManager[]{trustManager}, null);
      builder.sslContext(sslContext);
    }

    if (!config.httpProxyHost.isBlank()) {
      builder.proxy(ProxySelector.of(new InetSocketAddress(config.httpProxyHost, config.httpProxyPort)));
      LOG.info("added proxy with ", config.httpProxyHost, config.httpProxyPort);
    }
    LOG.info("building http client");
    httpClient = builder.build();
    LOG.info("built http client");
  }

  public CheckmarxResult<NotificationSettings> getNotificationSettings(String org) throws java.io.IOException, java.lang.InterruptedException {
    HttpRequest request = CheckmarxHttpRequestBuilder.create(config)
      .withPath(String.format(
        "user/me/notification-settings/org/%s",
        URLEncoder.encode(org, UTF_8)
      ))
      .build();
    LOG.info("getNotificationSettings created CheckmarxHttpRequestBuilder");
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    LOG.info("getNotificationSettings sent response");
    return CheckmarxResult.createResult(response, NotificationSettings.class);
  }

  public CheckmarxResult<TestResult> testMaven(String groupId, String artifactId, String version) throws Exception {
    String type = "mvn";
    String pkgName = String.format("%s:%s",groupId,artifactId);
    return getQueryResults(pkgName, version, type);
  }

  public CheckmarxResult<TestResult> testNpm(String packageName, String version) throws Exception {

      String type = "npm";
    return getQueryResults(packageName, version, type);
  }

  public CheckmarxResult<TestResult> testRubyGems(String gemName, String version, Optional<String> organisation) throws IOException, InterruptedException {
    HttpRequest request = CheckmarxHttpRequestBuilder.create(config)
      .withPath(String.format(
        "test/rubygems/%s/%s",
        URLEncoder.encode(gemName, UTF_8),
        URLEncoder.encode(version, UTF_8)
      ))
      .withQueryParam("org", organisation)
      .build();
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    return CheckmarxResult.createResult(response, TestResult.class);
  }


  public CheckmarxResult<TestResult> testPip(String packageName, String version) throws Exception {

    String type = "pypi";
    return getQueryResults(packageName, version, type);
  }

  public CheckmarxResult<TestResult> testNugget(String packageName, String version) throws Exception {

    String type = "nuget";
    return getQueryResults(packageName, version, type);
  }

  public static InputStream getDecodedInputStream(
    HttpResponse<InputStream> httpResponse) throws Exception {
    String encoding = determineContentEncoding(httpResponse);
    try {
      switch (encoding) {
        case "":
          return httpResponse.body();
        case "gzip":
          return new GZIPInputStream(httpResponse.body());
        default:
          throw new UnsupportedOperationException(
            "Unexpected Content-Encoding: " + encoding);
      }
    } catch (IOException ioe) {
      throw new Exception(ioe);
    }
  }

  public static String determineContentEncoding(
    HttpResponse<?> httpResponse) {
    return httpResponse.headers().firstValue("Content-Encoding").orElse("");
  }

  @NotNull
  private CheckmarxResult<TestResult> getQueryResults(String packageName, String version, String type) throws Exception {
    String str = String.format("[{\"type\":\"%s\",\"name\":\"%s\",\"version\":\"%s\"}]", type, packageName, version);

    HttpRequest request = CheckmarxHttpRequestBuilder.create(config)
      .build(HttpRequest.BodyPublishers.ofString(str));
    HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
    LOG.debug("sent request to api and got response " + response);
    if (response.statusCode() == 401)
      throw new Exception("Unauthorized");
    if(response.statusCode() == 400)
      throw new Exception("invalid request");

    String decodedResponse = (new String(getDecodedInputStream(response).readAllBytes() , StandardCharsets.UTF_8));;

    isMalcious(decodedResponse);

    return CheckmarxResult.createResult(response, decodedResponse, response.statusCode(), TestResult.class);

  }

}
