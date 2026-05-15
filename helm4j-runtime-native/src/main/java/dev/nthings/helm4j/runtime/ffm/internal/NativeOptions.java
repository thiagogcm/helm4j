package dev.nthings.helm4j.runtime.ffm.internal;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import dev.nthings.helm4j.chart.ChartRef;
import dev.nthings.helm4j.chart.ChartSource;
import dev.nthings.helm4j.chart.DependencyRequest;
import dev.nthings.helm4j.chart.LintRequest;
import dev.nthings.helm4j.chart.PackageChartRequest;
import dev.nthings.helm4j.chart.PullRequest;
import dev.nthings.helm4j.chart.PushRequest;
import dev.nthings.helm4j.chart.SearchCharts;
import dev.nthings.helm4j.chart.SearchHub;
import dev.nthings.helm4j.chart.ShowRequest;
import dev.nthings.helm4j.chart.TemplateRequest;
import dev.nthings.helm4j.registry.RegistryLogin;
import dev.nthings.helm4j.release.ApplyStrategy;
import dev.nthings.helm4j.release.DryRunMode;
import dev.nthings.helm4j.release.GetRelease;
import dev.nthings.helm4j.release.InstallRelease;
import dev.nthings.helm4j.release.ListReleases;
import dev.nthings.helm4j.release.ReleaseHistory;
import dev.nthings.helm4j.release.RollbackRelease;
import dev.nthings.helm4j.release.StatusRelease;
import dev.nthings.helm4j.release.TestRelease;
import dev.nthings.helm4j.release.UninstallRelease;
import dev.nthings.helm4j.release.UpgradeRelease;
import dev.nthings.helm4j.release.WaitMode;
import dev.nthings.helm4j.repository.AddRepository;
import dev.nthings.helm4j.repository.RemoveRepository;
import dev.nthings.helm4j.repository.UpdateRepositories;

import org.jspecify.annotations.Nullable;

/** Canonical native option-map builders for Helm bridge operations. */
final class NativeOptions {

  private NativeOptions() {}

  static Map<String, Object> repoAdd(AddRepository request) {
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

  static Map<String, Object> repoUpdate(UpdateRepositories request) {
    var options = options();
    options.put("names", request.names());
    putIfNonNull(options, "timeout", durationString(request.timeout()));
    return options;
  }

  static Map<String, Object> repoRemove(RemoveRepository request) {
    var options = options();
    options.put("names", request.names());
    return options;
  }

  static Map<String, Object> searchRepo(SearchCharts request) {
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

  static Map<String, Object> searchHub(SearchHub request) {
    var options = options();
    putIfNonNull(options, "keyword", request.keyword());
    putIfNonNull(options, "endpoint", request.endpoint());
    options.put("failOnNoResult", request.failIfNoResults());
    options.put("listRepoUrl", request.listRepositoryUrl());
    options.put("maxColWidth", request.maxColumnWidth());
    return options;
  }

  // ShowRequest carries no ChartRef — the show gateway holds it alongside the
  // mode — so the
  // chart is passed in separately here rather than read from the request like the
  // others.
  static Map<String, Object> show(ChartRef chart, ShowRequest request) {
    var options = options();
    putChartRef(options, chart);
    putChartSource(options, request.source());
    putIfNonNull(options, "jsonpath", request.valuesJsonPath());
    return options;
  }

  static Map<String, Object> install(InstallRelease request) {
    var options = options();
    putChartRef(options, request.chart());
    putChartSource(options, request.source());

    putIfNonNull(options, "namespace", request.namespace());
    options.put("createNamespace", request.createNamespace());
    putIfNonNull(options, "dryRun", dryRunModeValue(request.dryRun()));
    options.put("wait", waitModeWireValue(request.waitMode()));
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

  static Map<String, Object> upgrade(UpgradeRelease request) {
    var options = options();
    putChartRef(options, request.chart());
    putChartSource(options, request.source());

    putIfNonNull(options, "namespace", request.namespace());
    options.put("install", request.install());
    putIfNonNull(options, "dryRun", dryRunModeValue(request.dryRun()));
    options.put("wait", waitModeWireValue(request.waitMode()));
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

  static Map<String, Object> uninstall(UninstallRelease request) {
    var options = options();
    putIfNonNull(options, "namespace", request.namespace());
    options.put("dryRun", request.dryRun());
    options.put("disableHooks", request.disableHooks());
    options.put("keepHistory", request.keepHistory());
    options.put("ignoreNotFound", request.ignoreNotFound());
    putIfNonNull(options, "timeout", durationString(request.timeout()));
    putIfNonNull(options, "description", request.description());
    options.put("wait", waitModeWireValue(request.waitMode()));
    putIfNonNull(options, "deletionPropagation", request.deletionPropagation());
    return options;
  }

  static Map<String, Object> status(StatusRelease request) {
    var options = options();
    putIfNonNull(options, "namespace", request.namespace());
    options.put("revision", request.revision());
    return options;
  }

  static Map<String, Object> rollback(RollbackRelease request) {
    var options = options();
    putIfNonNull(options, "namespace", request.namespace());
    options.put("revision", request.revision());
    putIfNonNull(options, "dryRun", dryRunModeValue(request.dryRun()));
    options.put("disableHooks", request.disableHooks());
    options.put("forceReplace", request.forceReplace());
    putIfNonNull(options, "timeout", durationString(request.timeout()));
    options.put("wait", waitModeWireValue(request.waitMode()));
    options.put("waitForJobs", request.waitForJobs());
    options.put("cleanupOnFail", request.cleanupOnFail());
    options.put("maxHistory", request.maxHistory());
    putApplyStrategy(options, request.applyStrategy());
    return options;
  }

  static Map<String, Object> history(ReleaseHistory request) {
    var options = options();
    putIfNonNull(options, "namespace", request.namespace());
    options.put("max", request.max());
    return options;
  }

  static Map<String, Object> get(GetRelease request) {
    var options = options();
    putIfNonNull(options, "namespace", request.namespace());
    options.put("revision", request.revision());
    options.put("allValues", request.allValues());
    return options;
  }

  static Map<String, Object> template(TemplateRequest request) {
    var options = options();
    putChartRef(options, request.chart());
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

    if (!request.apiVersions().isEmpty()) {
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
    putChartRef(options, request.chart());
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

  static Map<String, Object> list(ListReleases request) {
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

  static Map<String, Object> test(TestRelease request) {
    var options = options();
    putIfNonNull(options, "namespace", request.namespace());
    putIfNonNull(options, "timeout", durationString(request.timeout()));
    if (!request.filter().isEmpty()) {
      options.put("filter", request.filter());
    }
    return options;
  }

  static Map<String, Object> registryLogin(RegistryLogin request) {
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

  private static void putChartRef(Map<String, Object> options, ChartRef chart) {
    putIfNonNull(options, "version", chart.version());
  }

  private static void putChartSource(Map<String, Object> options, ChartSource source) {
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

  private static @Nullable String dryRunModeValue(@Nullable DryRunMode dryRunMode) {
    return dryRunMode == null ? null : dryRunMode.wireValue();
  }

  // Helm v4 errors when "wait" is omitted; default to WATCHER so the SDK contract
  // that an unspecified strategy yields Helm's documented default still holds.
  private static String waitModeWireValue(@Nullable WaitMode waitMode) {
    return (waitMode == null ? WaitMode.WATCHER : waitMode).wireValue();
  }

  private static LinkedHashMap<String, Object> options() {
    return new LinkedHashMap<>();
  }

  private static void putIfNonNull(Map<String, Object> target, String key, @Nullable Object value) {
    if (value != null) {
      target.put(key, value);
    }
  }

  private static @Nullable String durationString(@Nullable Duration value) {
    if (value == null) {
      return null;
    }
    return value.toMillis() + "ms";
  }
}
