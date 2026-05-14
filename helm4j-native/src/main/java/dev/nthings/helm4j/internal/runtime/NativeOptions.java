package dev.nthings.helm4j.internal.runtime;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import dev.nthings.helm4j.chart.ChartSource;
import dev.nthings.helm4j.chart.DependencyRequest;
import dev.nthings.helm4j.chart.HubSearchRequest;
import dev.nthings.helm4j.chart.LintRequest;
import dev.nthings.helm4j.chart.PackageChartRequest;
import dev.nthings.helm4j.chart.PullRequest;
import dev.nthings.helm4j.chart.PushRequest;
import dev.nthings.helm4j.chart.RepoSearchRequest;
import dev.nthings.helm4j.chart.ShowRequest;
import dev.nthings.helm4j.chart.TemplateRequest;
import dev.nthings.helm4j.release.ApplyStrategy;
import dev.nthings.helm4j.release.DryRunMode;
import dev.nthings.helm4j.release.GetRequest;
import dev.nthings.helm4j.release.HistoryRequest;
import dev.nthings.helm4j.release.InstallRequest;
import dev.nthings.helm4j.release.ReleaseListRequest;
import dev.nthings.helm4j.release.RollbackRequest;
import dev.nthings.helm4j.release.StatusRequest;
import dev.nthings.helm4j.release.TestRequest;
import dev.nthings.helm4j.release.UninstallRequest;
import dev.nthings.helm4j.release.UpgradeRequest;
import dev.nthings.helm4j.release.WaitMode;
import dev.nthings.helm4j.repo.RegistryLoginRequest;
import dev.nthings.helm4j.repo.RepoAddRequest;
import dev.nthings.helm4j.repo.RepoRemoveRequest;
import dev.nthings.helm4j.repo.RepoUpdateRequest;

/** Canonical native option-map builders for Helm bridge operations. */
final class NativeOptions {

  private NativeOptions() {}

  static Map<String, Object> repoAdd(RepoAddRequest request) {
    var options = options();
    putIfNonNull(options, "name", request.name());
    putIfNonNull(options, "url", request.url());
    putIfNonNull(options, "username", request.username());
    putIfNonNull(options, "password", request.password());
    putIfNonNull(options, "certFile", request.certificateFile());
    putIfNonNull(options, "keyFile", request.keyFile());
    putIfNonNull(options, "caFile", request.certificateAuthorityFile());
    options.put("insecureSkipTlsVerify", request.insecureSkipTlsVerification());
    options.put("passCredentialsAll", request.passCredentialsToAllHosts());
    options.put("forceUpdate", request.forceUpdate());
    options.put("allowDeprecatedRepos", request.allowDeprecatedRepositories());
    putIfNonNull(options, "timeout", durationString(request.timeout()));
    return options;
  }

  static Map<String, Object> repoUpdate(RepoUpdateRequest request) {
    var options = options();
    options.put("names", request.names());
    putIfNonNull(options, "timeout", durationString(request.timeout()));
    return options;
  }

  static Map<String, Object> repoRemove(RepoRemoveRequest request) {
    var options = options();
    options.put("names", request.names());
    return options;
  }

  static Map<String, Object> searchRepo(RepoSearchRequest request) {
    var options = options();
    putIfNonNull(options, "keyword", request.keyword());
    options.put("regexp", request.regularExpression());
    options.put("versions", request.includeAllVersions());
    options.put("devel", request.includePreReleaseVersions());
    putIfNonNull(options, "version", request.versionConstraint());
    options.put("failOnNoResult", request.failIfNoResults());
    options.put("maxColWidth", request.maxColumnWidth());
    return options;
  }

  static Map<String, Object> searchHub(HubSearchRequest request) {
    var options = options();
    putIfNonNull(options, "keyword", request.keyword());
    putIfNonNull(options, "endpoint", request.endpoint());
    options.put("failOnNoResult", request.failIfNoResults());
    options.put("listRepoUrl", request.listRepositoryUrl());
    options.put("maxColWidth", request.maxColumnWidth());
    return options;
  }

  static Map<String, Object> show(ShowRequest request) {
    var options = options();
    putChartSource(options, request.source());
    putIfNonNull(options, "jsonpath", request.valuesJsonPath());
    return options;
  }

  static Map<String, Object> install(InstallRequest request) {
    var options = options();
    putChartSource(options, request.source());

    putIfNonNull(options, "namespace", request.namespace());
    options.put("createNamespace", request.createNamespace());
    putIfNonNull(options, "dryRun", dryRunModeValue(request.dryRunMode()));
    putIfNonNull(options, "wait", waitModeValue(request.waitMode()));
    options.put("waitForJobs", request.waitForJobs());
    putIfNonNull(options, "timeout", durationString(request.timeout()));
    putIfNonNull(options, "description", request.description());
    options.put("rollbackOnFailure", request.rollbackOnFailure());
    options.put("skipCrds", request.skipCrds());
    options.put("disableHooks", request.disableHooks());
    options.put("disableOpenApiValidation", request.disableOpenApiValidation());
    options.put("forceReplace", request.forceReplace());
    putApplyStrategy(options, request.applyStrategy());
    options.put("replace", request.replace());
    options.put("generateName", request.generateName());
    putIfNonNull(options, "nameTemplate", request.nameTemplate());
    options.put("subNotes", request.subNotes());
    options.put("enableDns", request.enableDns());
    options.put("takeOwnership", request.takeOwnership());
    options.put("dependencyUpdate", request.dependencyUpdate());

    if (!request.values().isEmpty()) {
      options.put("values", request.values());
    }
    if (!request.labels().isEmpty()) {
      options.put("labels", request.labels());
    }

    return options;
  }

  static Map<String, Object> upgrade(UpgradeRequest request) {
    var options = options();
    putChartSource(options, request.source());

    putIfNonNull(options, "namespace", request.namespace());
    options.put("install", request.install());
    putIfNonNull(options, "dryRun", dryRunModeValue(request.dryRunMode()));
    putIfNonNull(options, "wait", waitModeValue(request.waitMode()));
    options.put("waitForJobs", request.waitForJobs());
    putIfNonNull(options, "timeout", durationString(request.timeout()));
    putIfNonNull(options, "description", request.description());
    options.put("rollbackOnFailure", request.rollbackOnFailure());
    options.put("skipCrds", request.skipCrds());
    options.put("disableHooks", request.disableHooks());
    options.put("disableOpenApiValidation", request.disableOpenApiValidation());
    options.put("forceReplace", request.forceReplace());
    putApplyStrategy(options, request.applyStrategy());
    options.put("subNotes", request.subNotes());
    options.put("enableDns", request.enableDns());
    options.put("takeOwnership", request.takeOwnership());
    options.put("dependencyUpdate", request.dependencyUpdate());
    options.put("cleanupOnFail", request.cleanupOnFail());
    options.put("maxHistory", request.maxHistory());
    options.put("reuseValues", request.reuseValues());
    options.put("resetValues", request.resetValues());
    options.put("resetThenReuseValues", request.resetThenReuseValues());

    if (!request.values().isEmpty()) {
      options.put("values", request.values());
    }
    if (!request.labels().isEmpty()) {
      options.put("labels", request.labels());
    }

    return options;
  }

  static Map<String, Object> uninstall(UninstallRequest request) {
    var options = options();
    putIfNonNull(options, "namespace", request.namespace());
    options.put("dryRun", request.dryRun());
    options.put("disableHooks", request.disableHooks());
    options.put("keepHistory", request.keepHistory());
    options.put("ignoreNotFound", request.ignoreNotFound());
    putIfNonNull(options, "timeout", durationString(request.timeout()));
    putIfNonNull(options, "description", request.description());
    putIfNonNull(options, "wait", waitModeValue(request.waitMode()));
    putIfNonNull(options, "deletionPropagation", request.deletionPropagation());
    return options;
  }

  static Map<String, Object> status(StatusRequest request) {
    var options = options();
    putIfNonNull(options, "namespace", request.namespace());
    options.put("revision", request.revision());
    return options;
  }

  static Map<String, Object> rollback(RollbackRequest request) {
    var options = options();
    putIfNonNull(options, "namespace", request.namespace());
    options.put("revision", request.revision());
    putIfNonNull(options, "dryRun", dryRunModeValue(request.dryRunMode()));
    options.put("disableHooks", request.disableHooks());
    options.put("forceReplace", request.forceReplace());
    putIfNonNull(options, "timeout", durationString(request.timeout()));
    putIfNonNull(options, "wait", waitModeValue(request.waitMode()));
    options.put("waitForJobs", request.waitForJobs());
    options.put("cleanupOnFail", request.cleanupOnFail());
    options.put("maxHistory", request.maxHistory());
    putApplyStrategy(options, request.applyStrategy());
    return options;
  }

  static Map<String, Object> history(HistoryRequest request) {
    var options = options();
    putIfNonNull(options, "namespace", request.namespace());
    options.put("max", request.max());
    return options;
  }

  static Map<String, Object> get(GetRequest request) {
    var options = options();
    putIfNonNull(options, "namespace", request.namespace());
    options.put("revision", request.revision());
    options.put("allValues", request.allValues());
    return options;
  }

  static Map<String, Object> template(TemplateRequest request) {
    var options = options();
    putChartSource(options, request.source());

    putIfNonNull(options, "namespace", request.namespace());
    putIfNonNull(options, "description", request.description());
    options.put("skipCrds", request.skipCrds());
    options.put("disableHooks", request.disableHooks());
    options.put("disableOpenApiValidation", request.disableOpenApiValidation());
    options.put("generateName", request.generateName());
    putIfNonNull(options, "nameTemplate", request.nameTemplate());
    options.put("subNotes", request.subNotes());
    options.put("enableDns", request.enableDns());
    options.put("includeCrds", request.includeCrds());

    if (request.apiVersions() != null && !request.apiVersions().isEmpty()) {
      options.put("apiVersions", request.apiVersions());
    }
    if (!request.values().isEmpty()) {
      options.put("values", request.values());
    }
    if (!request.labels().isEmpty()) {
      options.put("labels", request.labels());
    }

    return options;
  }

  static Map<String, Object> lint(LintRequest request) {
    var options = options();
    options.put("strict", request.strict());
    options.put("quiet", request.quiet());
    options.put("withSubcharts", request.withSubcharts());
    if (!request.values().isEmpty()) {
      options.put("values", request.values());
    }
    return options;
  }

  static Map<String, Object> pull(PullRequest request) {
    var options = options();
    putChartSource(options, request.source());

    options.put("untar", request.untar());
    putIfNonNull(
        options,
        "untarDir",
        request.untarDirectory() == null ? null : request.untarDirectory().toString());
    putIfNonNull(
        options,
        "destDir",
        request.destinationDirectory() == null ? null : request.destinationDirectory().toString());

    return options;
  }

  static Map<String, Object> push(PushRequest request) {
    var options = options();
    options.put("plainHttp", request.plainHttp());
    options.put("insecureSkipTlsVerify", request.insecureSkipTlsVerification());
    putIfNonNull(options, "certFile", request.certificateFile());
    putIfNonNull(options, "keyFile", request.keyFile());
    putIfNonNull(options, "caFile", request.certificateAuthorityFile());
    return options;
  }

  static Map<String, Object> packageChart(PackageChartRequest request) {
    var options = options();
    putIfNonNull(options, "version", request.version());
    putIfNonNull(options, "appVersion", request.appVersion());
    putIfNonNull(
        options,
        "destination",
        request.destination() == null ? null : request.destination().toString());
    options.put("dependencyUpdate", request.dependencyUpdate());
    options.put("sign", request.sign());
    putIfNonNull(options, "key", request.key());
    putIfNonNull(options, "keyring", request.keyring());
    putIfNonNull(options, "passphraseFile", request.passphraseFile());
    options.put("plainHttp", request.plainHttp());
    options.put("insecureSkipTlsVerify", request.insecureSkipTlsVerification());
    putIfNonNull(options, "certFile", request.certificateFile());
    putIfNonNull(options, "keyFile", request.keyFile());
    putIfNonNull(options, "caFile", request.certificateAuthorityFile());
    return options;
  }

  static Map<String, Object> dependency(DependencyRequest request) {
    var options = options();
    options.put("skipRefresh", request.skipRefresh());
    options.put("verify", request.verify());
    putIfNonNull(options, "keyring", request.keyring());
    options.put("plainHttp", request.plainHttp());
    options.put("insecureSkipTlsVerify", request.insecureSkipTlsVerification());
    putIfNonNull(options, "certFile", request.certificateFile());
    putIfNonNull(options, "keyFile", request.keyFile());
    putIfNonNull(options, "caFile", request.certificateAuthorityFile());
    return options;
  }

  static Map<String, Object> list(ReleaseListRequest request) {
    var options = options();
    putIfNonNull(options, "namespace", request.namespace());
    options.put("allNamespaces", request.allNamespaces());
    putIfNonNull(options, "filter", request.filter());
    if (!request.states().isEmpty()) {
      options.put("states", request.states());
    }
    options.put("limit", request.limit());
    options.put("offset", request.offset());
    options.put("sortByDate", request.sortByDate());
    options.put("sortReverse", request.sortReverse());
    putIfNonNull(options, "selector", request.selector());
    return options;
  }

  static Map<String, Object> test(TestRequest request) {
    var options = options();
    putIfNonNull(options, "namespace", request.namespace());
    putIfNonNull(options, "timeout", durationString(request.timeout()));
    if (!request.filter().isEmpty()) {
      options.put("filter", request.filter());
    }
    return options;
  }

  static Map<String, Object> registryLogin(RegistryLoginRequest request) {
    var options = options();
    putIfNonNull(options, "username", request.username());
    putIfNonNull(options, "password", request.password());
    putIfNonNull(options, "certFile", request.certificateFile());
    putIfNonNull(options, "keyFile", request.keyFile());
    putIfNonNull(options, "caFile", request.certificateAuthorityFile());
    options.put("insecure", request.insecure());
    options.put("plainHttp", request.plainHttp());
    return options;
  }

  private static void putChartSource(Map<String, Object> options, ChartSource source) {
    putIfNonNull(options, "version", source.version());
    putIfNonNull(options, "repo", source.repositoryUrl());
    putIfNonNull(options, "username", source.username());
    putIfNonNull(options, "password", source.password());
    options.put("plainHttp", source.plainHttp());
    options.put("insecureSkipTlsVerify", source.insecureSkipTlsVerification());
    putIfNonNull(options, "keyring", source.keyringPath());
    putIfNonNull(options, "certFile", source.certificateFile());
    putIfNonNull(options, "keyFile", source.keyFile());
    putIfNonNull(options, "caFile", source.certificateAuthorityFile());
    options.put("passCredentialsAll", source.passCredentialsToAllHosts());
    options.put("verify", source.verifySignatures());
    options.put("devel", source.includePreReleaseVersions());
  }

  private static void putApplyStrategy(Map<String, Object> options, ApplyStrategy applyStrategy) {
    options.put("forceConflicts", applyStrategy.forceConflicts());
    options.put("serverSideApply", applyStrategy.serverSideApply());
  }

  private static String dryRunModeValue(DryRunMode dryRunMode) {
    return dryRunMode == null ? null : dryRunMode.wireValue();
  }

  private static String waitModeValue(WaitMode waitMode) {
    return waitMode == null ? null : waitMode.wireValue();
  }

  private static LinkedHashMap<String, Object> options() {
    return new LinkedHashMap<>();
  }

  private static void putIfNonNull(Map<String, Object> target, String key, Object value) {
    if (value != null) {
      target.put(key, value);
    }
  }

  private static String durationString(Duration value) {
    if (value == null) {
      return null;
    }
    return value.toMillis() + "ms";
  }
}
