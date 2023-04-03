package io.scs.plugins.artifactory.exception;

import io.scs.sdk.api.v1.scsResult;
import io.scs.sdk.model.TestResult;

public class ScsAPIFailureException extends RuntimeException {
  public ScsAPIFailureException(scsResult<TestResult> result) {
    super("scs API request was not successful. (" + result.statusCode + ")");
  }

  public ScsAPIFailureException(Exception cause) {
    super("scs API request encountered an unexpected error.", cause);
  }
}
