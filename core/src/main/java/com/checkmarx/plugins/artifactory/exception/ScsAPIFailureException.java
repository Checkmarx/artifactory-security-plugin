package com.checkmarx.plugins.artifactory.exception;

import com.checkmarx.sdk.api.v1.scsResult;
import com.checkmarx.sdk.model.TestResult;

public class ScsAPIFailureException extends RuntimeException {
  public ScsAPIFailureException(scsResult<TestResult> result) {
    super("scs API request was not successful. (" + result.statusCode + ")");
  }

  public ScsAPIFailureException(Exception cause) {
    super("scs API request encountered an unexpected error.", cause);
  }
}
