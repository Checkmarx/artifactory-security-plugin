package io.scs.sdk.api.v1;

import io.scs.sdk.ScsConfig;

import javax.annotation.Nonnull;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;


public class ScsHttpRequestBuilder {
  private final ScsConfig config;
  private final HashMap<String, String> queryParams = new HashMap<>();
  private String path = "";

  private ScsHttpRequestBuilder(@Nonnull ScsConfig config) {
    this.config = config;
  }

  public static ScsHttpRequestBuilder create(@Nonnull ScsConfig config) {
    return new ScsHttpRequestBuilder(config);
  }

  public ScsHttpRequestBuilder withPath(@Nonnull String path) {
    this.path = path;
    return this;
  }

  public ScsHttpRequestBuilder withQueryParam(String key, String value) {
    return withQueryParam(key, Optional.ofNullable(value));
  }

  public ScsHttpRequestBuilder withQueryParam(String key, Optional<String> value) {
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
