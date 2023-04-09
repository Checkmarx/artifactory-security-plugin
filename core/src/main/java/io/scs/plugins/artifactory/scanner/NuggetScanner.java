package io.scs.plugins.artifactory.scanner;

import io.scs.sdk.model.TestResult;
import io.scs.plugins.artifactory.configuration.ConfigurationModule;
import io.scs.plugins.artifactory.exception.CannotScanException;
import io.scs.plugins.artifactory.exception.ScsAPIFailureException;
import io.scs.sdk.api.v1.scsClient;
import io.scs.sdk.api.v1.scsResult;
import org.artifactory.fs.FileLayoutInfo;
import org.artifactory.repo.RepoPath;
import org.artifactory.request.Request;
import org.slf4j.Logger;

import java.net.URLEncoder;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.slf4j.LoggerFactory.getLogger;

public class NuggetScanner implements PackageScanner{


  private static final Logger LOG = getLogger(NuggetScanner.class);

  private final ConfigurationModule configurationModule;
  private final scsClient scsClient;

  NuggetScanner(ConfigurationModule configurationModule, scsClient scsClient) {
    this.configurationModule = configurationModule;
    this.scsClient = scsClient;
  }

  public static Optional<NuggetScanner.ModuleURLDetails> getModuleDetailsFromFileLayoutInfo(FileLayoutInfo fileLayoutInfo) {
    String module = fileLayoutInfo.getModule();
    String baseRevision = fileLayoutInfo.getBaseRevision();
    if (module == null || baseRevision == null) {
      return Optional.empty();
    }
    return Optional.of(new NuggetScanner.ModuleURLDetails(
      module,
      baseRevision
    ));
  }

  public static Optional<NuggetScanner.ModuleURLDetails> getModuleDetailsFromUrl(String repoPath) {

    final String regex = "((0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?)(\\.nupkg)";

    final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
    final Matcher matcher = pattern.matcher(repoPath);
    String version;
    String name;
    matcher.find();
    version =  matcher.group(1);
    name = repoPath.split("." + version,-1)[0];
    name = name.split(":",-1)[1];
    System.out.println(name  + " " + version);

    LOG.debug("name is:  " + name + " version is " + version);
    if (name != "" && version!= "") {
      return Optional.of(new NuggetScanner.ModuleURLDetails(
        name,
        version
      ));
    }
    return Optional.empty();
  }

  public static Optional<NuggetScanner.ModuleURLDetails> getModuleDetailsFromRequest(Request request) {
    LOG.debug("Repo uri is " + request.getUri());
    LOG.debug("Repo request " + request.toString());
//    Pattern pattern = Pattern.compile("^.+:.+/.+/.+/(?<packageName>.+)-(?<packageVersion>\\d+(?:\\.[A-Za-z0-9]+)*).*\\.(?:nupkg)$");
//    Matcher matcher = pattern.matcher(request.getUri());
//    if (matcher.matches()) {
      String packageName;
      String packageVersion;
      String[] arrOfStr = request.getUri().split("/", -2);
      if (arrOfStr.length > 0)
    {
      packageName = arrOfStr[arrOfStr.length - 1];
      packageVersion = arrOfStr[arrOfStr.length - 2];
      return Optional.of(new NuggetScanner.ModuleURLDetails(
        packageName,
        packageVersion
      ));
    }
    else
    return Optional.empty();
  }

  public TestResult scan(FileLayoutInfo fileLayoutInfo, RepoPath repoPath, Request request) {
    NuggetScanner.ModuleURLDetails details = getModuleDetailsFromFileLayoutInfo(fileLayoutInfo)
      .orElseGet(() -> getModuleDetailsFromUrl(repoPath.toString())
      .orElseGet(() -> getModuleDetailsFromRequest(request)
        .orElseThrow(() -> new CannotScanException("Module details not provided."))));

    scsResult<TestResult> result;
    try {
      result = scsClient.testNugget(
        details.name,
        details.version
      );
    } catch (Exception e) {
      if (!(e.toString().contains("Unsafe package")))
        LOG.error("error in scan nuget package module nugetscanner: " + e);
      throw new ScsAPIFailureException(e);
    }

    TestResult testResult = result.get().orElseThrow(() -> new ScsAPIFailureException(result));
//    testResult.packageDetailsURL = getModuleDetailsURL(details);
    return testResult;
  }

  public static class ModuleURLDetails {
    public final String name;
    public final String version;

    public ModuleURLDetails(String name, String version) {
      this.name = name;
      this.version = version;
    }
  }

}
