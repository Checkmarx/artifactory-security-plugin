package com.checkmarx.plugins.artifactory.scanner;

import com.checkmarx.sdk.model.TestResult;
import org.artifactory.fs.FileLayoutInfo;
import org.artifactory.repo.RepoPath;
import org.artifactory.request.Request;

interface PackageScanner {
  TestResult scan(FileLayoutInfo fileLayoutInfo, RepoPath repoPath, Request request);
}
