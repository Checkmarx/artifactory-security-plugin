package com.checkmarx.plugins.artifactory.exception;

import com.checkmarx.sdk.api.v1.CheckmarxResult;
import com.checkmarx.sdk.model.TestResult;

public class CheckmarxAPIFailureException extends RuntimeException {
  public CheckmarxAPIFailureException(CheckmarxResult<TestResult> result) {
    super("Checkmarx API request was not successful. (" + result.statusCode + ")");
  }

  public CheckmarxAPIFailureException(Exception cause) {
    super("Checkmarx API request encountered an unexpected error.", cause);
  }
}
