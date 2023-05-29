package com.checkmarx.sdk.api.v1;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.Optional;

public class CheckmarxResult<T> {
  public int statusCode;
  public Optional<T> result = Optional.empty();
  public Optional<String> responseAsText = Optional.empty();
  public HttpResponse<String> response;
  private static final Logger LOG = LoggerFactory.getLogger(CheckmarxResult.class);

  public CheckmarxResult(int statusCode, T result, String responseBody, HttpResponse<String> response) {
    this.statusCode = statusCode;
    this.result = Optional.of(result);
    this.responseAsText = Optional.of(responseBody);
    this.response = response;
  }

  public CheckmarxResult(HttpResponse<String> response) {
    this.statusCode = response.statusCode();
    this.responseAsText = Optional.of(response.body());
    this.response = response;
  }

  public Optional<T> get() {
    return this.result;
  }

  public boolean isSuccessful() {
    return statusCode == 200;
  }

  public static <ResType> CheckmarxResult<ResType> createResult(HttpResponse<String> response, Class<ResType> resultType) throws IOException {
    int status = response.statusCode();
    if (status == 200) {
      String responseBody = response.body().substring(1,response.body().length() - 1);
      ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      var res = objectMapper.readValue(responseBody, resultType);
      return new CheckmarxResult<>(status, res, responseBody, response);
    } else {
      return new CheckmarxResult<>(response);
    }
  }
  public static <ResType> CheckmarxResult<ResType> createResult(HttpResponse response, String responseString, int status, Class<ResType> resultType) throws IOException {
    if (status == 200) {
      String responseBody = responseString.substring(1,responseString.length() - 1);
      ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      var res = objectMapper.readValue(responseBody, resultType);
      return new CheckmarxResult<>(status, res, responseBody, response);
    } else {
      return new CheckmarxResult<>(response);
    }
  }
}
