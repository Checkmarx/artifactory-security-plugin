package io.scs.plugins.artifactory.exception;

public class ScsRuntimeException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public ScsRuntimeException(String message) {
    super(message);
  }

  public ScsRuntimeException(String message, Throwable cause) {
    super(message, cause);
  }
}
