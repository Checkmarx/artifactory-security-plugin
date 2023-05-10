package com.checkmarx.plugins.artifactory.exception;

public class CannotScanException extends RuntimeException {
  public CannotScanException(String reason) {
    super(reason);
  }
}
