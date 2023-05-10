package com.checkmarx.sdk.model;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The test result is the object returned from the API giving the results of testing a package
 * for issues.
 */
public class TestResult implements Serializable {

  private static final long serialVersionUID = 1L;

//  @JsonProperty("ok")
//  public boolean success;
//  @JsonProperty("dependencyCount")
//  public int dependencyCount;
//  @JsonProperty("org")
//  public Organisation organisation;
  @JsonProperty("type")
  public String packageManager;

  @JsonProperty("name")
  public String name;

  @JsonProperty("version")
  public String version;

  @JsonProperty("ioc")
  public List<String> ioc;

  @JsonProperty("risks")
  public List<String> risks;



//  public String packageDetailsURL;
}
