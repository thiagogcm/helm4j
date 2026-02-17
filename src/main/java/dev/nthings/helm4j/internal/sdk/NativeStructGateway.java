package dev.nthings.helm4j.internal.sdk;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import dev.nthings.helm4j.chart.HubChartSummary;
import dev.nthings.helm4j.chart.HubSearchRequest;
import dev.nthings.helm4j.chart.HubSearchResult;
import dev.nthings.helm4j.chart.RepoChartSummary;
import dev.nthings.helm4j.chart.RepoSearchRequest;
import dev.nthings.helm4j.chart.RepoSearchResult;
import dev.nthings.helm4j.chart.ShowAllResult;
import dev.nthings.helm4j.chart.ShowChartResult;
import dev.nthings.helm4j.chart.ShowCrdsResult;
import dev.nthings.helm4j.chart.ShowMode;
import dev.nthings.helm4j.chart.ShowReadmeResult;
import dev.nthings.helm4j.chart.ShowRequest;
import dev.nthings.helm4j.chart.ShowValuesResult;
import dev.nthings.helm4j.errors.HelmException;
import dev.nthings.helm4j.release.InstallFailure;
import dev.nthings.helm4j.release.InstallPending;
import dev.nthings.helm4j.release.InstallRequest;
import dev.nthings.helm4j.release.InstallResult;
import dev.nthings.helm4j.release.InstallSuccess;
import dev.nthings.helm4j.release.ReleaseInfo;
import dev.nthings.helm4j.repo.RepoAddFailure;
import dev.nthings.helm4j.repo.RepoAddRequest;
import dev.nthings.helm4j.repo.RepoAddResult;
import dev.nthings.helm4j.repo.RepoAddSuccess;
import dev.nthings.helm4j.repo.RepoListResult;
import dev.nthings.helm4j.repo.RepoRemoveRequest;
import dev.nthings.helm4j.repo.RepoRemoveResult;
import dev.nthings.helm4j.repo.RepoSummary;
import dev.nthings.helm4j.repo.RepoUpdateEntry;
import dev.nthings.helm4j.repo.RepoUpdateRequest;
import dev.nthings.helm4j.repo.RepoUpdateResult;
import dev.nthings.helm4j.types.ChartRef;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Gateway backed by JSON bridge functions exported by libhelm4j. */
public final class NativeStructGateway implements HelmGateway {

  private final NativeStructBridge bridge;
  private final ObjectMapper mapper;

  public NativeStructGateway(NativeStructBridge bridge, ObjectMapper mapper) {
    this.bridge = Objects.requireNonNull(bridge, "bridge");
    this.mapper = Objects.requireNonNull(mapper, "mapper");
  }

  @Override
  public RepoAddResult repoAdd(RepoAddRequest request) {
    Objects.requireNonNull(request, "request");
    if (request.name() == null || request.url() == null) {
      throw new IllegalArgumentException("Repository add requires non-null name and url");
    }

    var payload =
        invoke(
            () -> bridge.repo("add", toJson(repoAddOptions(request), "repo add")),
            "repo add",
            "invokeNative");
    var root = parse(payload, "repo add");

    var failure = operationError(root, "repo add");
    if (failure != null) {
      return new RepoAddFailure(
          messageOrUnknown(failure.message()), failure.stage(), failure.operation());
    }

    var response = convert(root, RepoAddPayload.class, "repo add");
    if (response == null || response.name() == null || response.url() == null) {
      throw new HelmException(
          "Native repo add response missing required fields", "decodeResponse", "repo add");
    }
    return new RepoAddSuccess(response.name(), response.url());
  }

  @Override
  public RepoUpdateResult repoUpdate(RepoUpdateRequest request) {
    Objects.requireNonNull(request, "request");

    var payload =
        invoke(
            () -> bridge.repo("update", toJson(repoUpdateOptions(request), "repo update")),
            "repo update",
            "invokeNative");
    var root = parse(payload, "repo update");

    var failure = operationError(root, "repo update");
    if (failure != null) {
      throw asException(failure);
    }

    var response = convert(root, RepoUpdatePayload.class, "repo update");
    var repositories =
        listOrEmpty(response == null ? null : response.repositories()).stream()
            .map(entry -> new RepoUpdateEntry(entry.name(), entry.status()))
            .toList();
    return new RepoUpdateResult(repositories);
  }

  @Override
  public RepoListResult repoList() {
    var payload =
        invoke(
            () -> bridge.repo("list", toJson(Map.of(), "repo list")), "repo list", "invokeNative");
    var root = parse(payload, "repo list");

    var failure = operationError(root, "repo list");
    if (failure != null) {
      throw asException(failure);
    }

    var response = convert(root, RepoListPayload.class, "repo list");
    var repositories =
        listOrEmpty(response == null ? null : response.repositories()).stream()
            .map(entry -> new RepoSummary(entry.name(), entry.url()))
            .toList();
    return new RepoListResult(repositories);
  }

  @Override
  public RepoRemoveResult repoRemove(RepoRemoveRequest request) {
    Objects.requireNonNull(request, "request");

    var payload =
        invoke(
            () -> bridge.repo("remove", toJson(repoRemoveOptions(request), "repo remove")),
            "repo remove",
            "invokeNative");
    var root = parse(payload, "repo remove");

    var failure = operationError(root, "repo remove");
    if (failure != null) {
      throw asException(failure);
    }

    var response = convert(root, RepoRemovePayload.class, "repo remove");
    return new RepoRemoveResult(listOrEmpty(response == null ? null : response.removed()));
  }

  @Override
  public RepoSearchResult searchRepo(RepoSearchRequest request) {
    Objects.requireNonNull(request, "request");

    var payload =
        invoke(
            () -> bridge.search("repo", toJson(searchRepoOptions(request), "search repo")),
            "search repo",
            "invokeNative");
    var root = parse(payload, "search repo");

    var failure = operationError(root, "search repo");
    if (failure != null) {
      throw asException(failure);
    }

    var response = convert(root, SearchPayload.class, "search repo");
    var charts =
        listOrEmpty(response == null ? null : response.results()).stream()
            .map(
                entry ->
                    new RepoChartSummary(
                        entry.name(),
                        entry.version(),
                        entry.appVersion(),
                        entry.description(),
                        entry.score(),
                        entry.repositoryName(),
                        entry.repositoryUrl()))
            .toList();
    return new RepoSearchResult(charts);
  }

  @Override
  public HubSearchResult searchHub(HubSearchRequest request) {
    Objects.requireNonNull(request, "request");

    var payload =
        invoke(
            () -> bridge.search("hub", toJson(searchHubOptions(request), "search hub")),
            "search hub",
            "invokeNative");
    var root = parse(payload, "search hub");

    var failure = operationError(root, "search hub");
    if (failure != null) {
      throw asException(failure);
    }

    var response = convert(root, SearchPayload.class, "search hub");
    var charts =
        listOrEmpty(response == null ? null : response.results()).stream()
            .map(
                entry ->
                    new HubChartSummary(
                        entry.name(),
                        entry.version(),
                        entry.appVersion(),
                        entry.description(),
                        entry.score(),
                        entry.url(),
                        entry.repositoryName(),
                        entry.repositoryUrl()))
            .toList();
    return new HubSearchResult(charts);
  }

  @Override
  public ShowChartResult showChart(ChartRef chartReference, ShowRequest request) {
    var response = runShow(ShowMode.CHART, chartReference, request);
    return new ShowChartResult(
        response.chartRef(),
        response.chartPath(),
        response.sections().chart(),
        response.cliOutput());
  }

  @Override
  public ShowValuesResult showValues(ChartRef chartReference, ShowRequest request) {
    var response = runShow(ShowMode.VALUES, chartReference, request);
    return new ShowValuesResult(
        response.chartRef(),
        response.chartPath(),
        response.sections().values(),
        response.cliOutput());
  }

  @Override
  public ShowReadmeResult showReadme(ChartRef chartReference, ShowRequest request) {
    var response = runShow(ShowMode.README, chartReference, request);
    return new ShowReadmeResult(
        response.chartRef(),
        response.chartPath(),
        response.sections().readme(),
        response.cliOutput());
  }

  @Override
  public ShowCrdsResult showCrds(ChartRef chartReference, ShowRequest request) {
    var response = runShow(ShowMode.CRDS, chartReference, request);
    return new ShowCrdsResult(
        response.chartRef(),
        response.chartPath(),
        listOrEmpty(response.sections().crds()),
        response.cliOutput());
  }

  @Override
  public ShowAllResult showAll(ChartRef chartReference, ShowRequest request) {
    var response = runShow(ShowMode.ALL, chartReference, request);
    return new ShowAllResult(
        response.chartRef(),
        response.chartPath(),
        response.sections().chart(),
        response.sections().values(),
        response.sections().readme(),
        listOrEmpty(response.sections().crds()),
        response.cliOutput());
  }

  @Override
  public InstallResult install(InstallRequest request) {
    Objects.requireNonNull(request, "request");
    if (request.chart() == null) {
      throw new IllegalArgumentException("Install requires chart reference");
    }

    var payload =
        invoke(
            () ->
                bridge.install(
                    request.releaseName(),
                    request.chart().asReference(),
                    toJson(installOptions(request), "install")),
            "install",
            "invokeNative");
    var root = parse(payload, "install");

    var failure = operationError(root, "install");
    if (failure != null) {
      return new InstallFailure(
          messageOrUnknown(failure.message()), failure.stage(), failure.operation());
    }

    var response = convert(root, InstallPayload.class, "install");
    if (response == null || response.release() == null) {
      throw new HelmException(
          "Native install response missing release", "decodeResponse", "install");
    }

    var nativeRelease = response.release();
    var release =
        new ReleaseInfo(
            nativeRelease.name(),
            nativeRelease.namespace(),
            nativeRelease.revision(),
            nativeRelease.status(),
            nativeRelease.description(),
            nativeRelease.firstDeployed(),
            nativeRelease.lastDeployed(),
            nativeRelease.chartName(),
            nativeRelease.chartVersion(),
            nativeRelease.appVersion(),
            nativeRelease.notes());

    if (isPendingStatus(release.status())) {
      return new InstallPending(release);
    }
    return new InstallSuccess(release);
  }

  private ShowPayload runShow(ShowMode mode, ChartRef chartReference, ShowRequest request) {
    Objects.requireNonNull(mode, "mode");
    Objects.requireNonNull(chartReference, "chartReference");
    Objects.requireNonNull(request, "request");

    var operation = "show " + mode.wireValue();
    var payload =
        invoke(
            () ->
                bridge.show(
                    mode.wireValue(),
                    chartReference.asReference(),
                    toJson(showOptions(request), operation)),
            operation,
            "invokeNative");
    var root = parse(payload, operation);

    var failure = operationError(root, operation);
    if (failure != null) {
      throw asException(failure);
    }

    var response = convert(root, ShowPayload.class, operation);
    if (response == null || response.sections() == null) {
      throw new HelmException("Native show response missing sections", "decodeResponse", operation);
    }

    if (response.mode() != null && !mode.wireValue().equals(response.mode())) {
      throw new HelmException("Native show response mode mismatch", "decodeResponse", operation);
    }

    return response;
  }

  private String invoke(StringInvocation invocation, String operation, String stage) {
    final String payload;
    try {
      payload = invocation.invoke();
    } catch (RuntimeException error) {
      throw new HelmException("Native bridge invocation failed", stage, operation, error);
    }

    if (payload == null || payload.isBlank()) {
      throw new HelmException("Native bridge returned empty response", stage, operation);
    }
    return payload;
  }

  private JsonNode parse(String payload, String operation) {
    try {
      return mapper.readTree(payload);
    } catch (JacksonException error) {
      throw new HelmException(
          "Failed to decode native response", "decodeResponse", operation, error);
    }
  }

  private <T> T convert(JsonNode node, Class<T> type, String operation) {
    try {
      return mapper.treeToValue(node, type);
    } catch (JacksonException error) {
      throw new HelmException(
          "Failed to decode native response", "decodeResponse", operation, error);
    }
  }

  private HelmException asException(OperationError error) {
    return new HelmException(messageOrUnknown(error.message()), error.stage(), error.operation());
  }

  private static OperationError operationError(JsonNode node, String fallbackOperation) {
    if (node == null || !node.has("error")) {
      return null;
    }

    var message = text(node, "error");
    if (message == null || message.isBlank()) {
      return null;
    }

    return new OperationError(
        message,
        text(node, "stage"),
        fallbackOperation(text(node, "operation"), fallbackOperation));
  }

  private static String text(JsonNode node, String field) {
    var value = node.get(field);
    if (value == null || value.isNull()) {
      return null;
    }
    return value.asText();
  }

  private String toJson(Map<String, Object> payload, String operation) {
    try {
      return mapper.writeValueAsString(payload);
    } catch (JacksonException error) {
      throw new HelmException("Failed to encode native options", "encodeOptions", operation, error);
    }
  }

  private static Map<String, Object> repoAddOptions(RepoAddRequest request) {
    var options = new LinkedHashMap<String, Object>();
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

  private static Map<String, Object> repoUpdateOptions(RepoUpdateRequest request) {
    var options = new LinkedHashMap<String, Object>();
    options.put("names", request.names());
    putIfNonNull(options, "timeout", durationString(request.timeout()));
    return options;
  }

  private static Map<String, Object> repoRemoveOptions(RepoRemoveRequest request) {
    var options = new LinkedHashMap<String, Object>();
    options.put("names", request.names());
    return options;
  }

  private static Map<String, Object> searchRepoOptions(RepoSearchRequest request) {
    var options = new LinkedHashMap<String, Object>();
    putIfNonNull(options, "keyword", request.keyword());
    options.put("regexp", request.regularExpression());
    options.put("versions", request.includeAllVersions());
    options.put("devel", request.includePreReleaseVersions());
    putIfNonNull(options, "version", request.versionConstraint());
    options.put("failOnNoResult", request.failIfNoResults());
    options.put("maxColWidth", request.maxColumnWidth());
    return options;
  }

  private static Map<String, Object> searchHubOptions(HubSearchRequest request) {
    var options = new LinkedHashMap<String, Object>();
    putIfNonNull(options, "keyword", request.keyword());
    putIfNonNull(options, "endpoint", request.endpoint());
    options.put("failOnNoResult", request.failIfNoResults());
    options.put("listRepoUrl", request.listRepositoryUrl());
    options.put("maxColWidth", request.maxColumnWidth());
    return options;
  }

  private static Map<String, Object> showOptions(ShowRequest request) {
    var source = request.source();
    var options = new LinkedHashMap<String, Object>();
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
    putIfNonNull(options, "jsonpath", request.valuesJsonPath());
    return options;
  }

  private static Map<String, Object> installOptions(InstallRequest request) {
    var source = request.source();
    var options = new LinkedHashMap<String, Object>();

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

    putIfNonNull(options, "namespace", request.namespace());
    options.put("createNamespace", request.createNamespace());
    putIfNonNull(
        options, "dryRun", request.dryRunMode() == null ? null : request.dryRunMode().wireValue());
    putIfNonNull(
        options, "wait", request.waitMode() == null ? null : request.waitMode().wireValue());
    options.put("waitForJobs", request.waitForJobs());
    putIfNonNull(options, "timeout", durationString(request.timeout()));
    putIfNonNull(options, "description", request.description());
    options.put("rollbackOnFailure", request.rollbackOnFailure());
    options.put("skipCrds", request.skipCrds());
    options.put("disableHooks", request.disableHooks());
    options.put("disableOpenApiValidation", request.disableOpenApiValidation());
    options.put("forceReplace", request.forceReplace());
    options.put("forceConflicts", request.applyStrategy().forceConflicts());
    options.put("serverSideApply", request.applyStrategy().serverSideApply());
    options.put("replace", request.replace());
    options.put("generateName", request.generateName());
    putIfNonNull(options, "nameTemplate", request.nameTemplate());
    options.put("subNotes", request.subNotes());
    options.put("enableDns", request.enableDns());
    options.put("takeOwnership", request.takeOwnership());

    if (!request.values().isEmpty()) {
      options.put("values", request.values());
    }
    if (!request.labels().isEmpty()) {
      options.put("labels", request.labels());
    }

    return options;
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

  private static String fallbackOperation(String operation, String fallback) {
    return operation == null || operation.isBlank() ? fallback : operation;
  }

  private static String messageOrUnknown(String message) {
    if (message == null || message.isBlank()) {
      return "Unknown native operation error";
    }
    return message;
  }

  private static boolean isPendingStatus(String status) {
    if (status == null) {
      return false;
    }
    var normalized = status.trim().toLowerCase(Locale.ROOT);
    return normalized.startsWith("pending");
  }

  private static <T> List<T> listOrEmpty(List<T> value) {
    return value == null ? List.of() : value;
  }

  @FunctionalInterface
  private interface StringInvocation {
    String invoke();
  }

  private record OperationError(String message, String stage, String operation) {}

  private record RepoAddPayload(String name, String url) {}

  private record RepoUpdatePayload(List<RepoUpdateEntryPayload> repositories) {}

  private record RepoUpdateEntryPayload(String name, String status) {}

  private record RepoListPayload(List<RepoListEntryPayload> repositories) {}

  private record RepoListEntryPayload(String name, String url) {}

  private record RepoRemovePayload(List<String> removed) {}

  private record SearchPayload(List<SearchResultPayload> results) {}

  private record SearchResultPayload(
      String name,
      String version,
      String appVersion,
      String description,
      int score,
      String url,
      String repositoryName,
      String repositoryUrl) {}

  private record ShowPayload(
      String mode,
      String chartRef,
      String chartPath,
      ShowSectionsPayload sections,
      String cliOutput) {}

  private record ShowSectionsPayload(
      String chart, String values, String readme, List<String> crds) {}

  private record InstallPayload(InstallReleasePayload release) {}

  private record InstallReleasePayload(
      String name,
      String namespace,
      int revision,
      String status,
      String description,
      String firstDeployed,
      String lastDeployed,
      String chartName,
      String chartVersion,
      String appVersion,
      String notes) {}
}
