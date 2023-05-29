package com.checkmarx.plugins.artifactory.exception;

public class CheckmarxRuntimeException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public CheckmarxRuntimeException(String message) {
    super(message);
  }

  public CheckmarxRuntimeException(String message, Throwable cause) {
    super(message, cause);
  }
}
