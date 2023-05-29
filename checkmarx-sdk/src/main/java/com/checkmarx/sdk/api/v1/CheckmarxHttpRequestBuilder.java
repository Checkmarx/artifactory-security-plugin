package com.checkmarx.sdk.api.v1;

import com.checkmarx.sdk.CheckmarxConfig;

import javax.annotation.Nonnull;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;


public class CheckmarxHttpRequestBuilder {
  private final CheckmarxConfig config;
  private final HashMap<String, String> queryParams = new HashMap<>();
  private String path = "";

  private CheckmarxHttpRequestBuilder(@Nonnull CheckmarxConfig config) {
    this.config = config;
  }

  public static CheckmarxHttpRequestBuilder create(@Nonnull CheckmarxConfig config) {
    return new CheckmarxHttpRequestBuilder(config);
  }

  public CheckmarxHttpRequestBuilder withPath(@Nonnull String path) {
    this.path = path;
    return this;
  }

  public CheckmarxHttpRequestBuilder withQueryParam(String key, String value) {
    return withQueryParam(key, Optional.ofNullable(value));
  }

  public CheckmarxHttpRequestBuilder withQueryParam(String key, Optional<String> value) {
    value.ifPresent(v -> this.queryParams.put(key, v));
    return this;
  }

  public HttpRequest build() {
    return HttpRequest.newBuilder()
      .GET()
      .uri(buildURI())
      .timeout(config.timeout)
      .setHeader("Authorization", String.format("token %s", config.token))
      .setHeader("User-Agent", config.userAgent)
      .build();
  }

  public HttpRequest build(HttpRequest.BodyPublisher body ) {
    return HttpRequest.newBuilder()
      .POST(body)
      .uri(URI.create(config.baseUrl))
      .timeout(config.timeout)
      .setHeader("Authorization", String.format("token %s", config.token))
      .setHeader("User-Agent", config.userAgent)
      .build();
  }
  private URI buildURI() {
    String apiUrl = config.baseUrl + path;

    String queryString = this.queryParams
      .entrySet()
      .stream()
      .map((entry) -> String.format(
        "%s=%s",
        URLEncoder.encode(entry.getKey(), UTF_8),
        URLEncoder.encode(entry.getValue(), UTF_8)))
      .collect(Collectors.joining("&"));

    if (!queryString.isBlank()) {
      apiUrl += "?" + queryString;
    }

    return URI.create(apiUrl);
  }
}
