package dev.nthings.helm4j.internal.sdk;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import dev.nthings.helm4j.VersionInfo;
import dev.nthings.helm4j.chart.ChartRef;
import dev.nthings.helm4j.chart.DependencyRequest;
import dev.nthings.helm4j.chart.DependencyResult;
import dev.nthings.helm4j.chart.HubChartSummary;
import dev.nthings.helm4j.chart.HubSearchRequest;
import dev.nthings.helm4j.chart.HubSearchResult;
import dev.nthings.helm4j.chart.LintMessage;
import dev.nthings.helm4j.chart.LintRequest;
import dev.nthings.helm4j.chart.LintResult;
import dev.nthings.helm4j.chart.LintSeverity;
import dev.nthings.helm4j.chart.PackageChartRequest;
import dev.nthings.helm4j.chart.PackageChartResult;
import dev.nthings.helm4j.chart.PullRequest;
import dev.nthings.helm4j.chart.PullResult;
import dev.nthings.helm4j.chart.PushRequest;
import dev.nthings.helm4j.chart.PushResult;
import dev.nthings.helm4j.chart.RepoChartSummary;
import dev.nthings.helm4j.chart.RepoSearchRequest;
import dev.nthings.helm4j.chart.RepoSearchResult;
import dev.nthings.helm4j.chart.ShowMode;
import dev.nthings.helm4j.chart.ShowRequest;
import dev.nthings.helm4j.chart.ShowResult;
import dev.nthings.helm4j.chart.TemplateRequest;
import dev.nthings.helm4j.chart.TemplateResult;
import dev.nthings.helm4j.errors.HelmException;
import dev.nthings.helm4j.release.GetAllResult;
import dev.nthings.helm4j.release.GetHooksResult;
import dev.nthings.helm4j.release.GetManifestResult;
import dev.nthings.helm4j.release.GetMetadataResult;
import dev.nthings.helm4j.release.GetMode;
import dev.nthings.helm4j.release.GetNotesResult;
import dev.nthings.helm4j.release.GetRequest;
import dev.nthings.helm4j.release.GetValuesResult;
import dev.nthings.helm4j.release.HistoryEntry;
import dev.nthings.helm4j.release.HistoryRequest;
import dev.nthings.helm4j.release.HistoryResult;
import dev.nthings.helm4j.release.HookInfo;
import dev.nthings.helm4j.release.InstallRequest;
import dev.nthings.helm4j.release.InstallResult;
import dev.nthings.helm4j.release.ReleaseFailure;
import dev.nthings.helm4j.release.ReleaseInfo;
import dev.nthings.helm4j.release.ReleaseListRequest;
import dev.nthings.helm4j.release.ReleaseListResult;
import dev.nthings.helm4j.release.ReleasePending;
import dev.nthings.helm4j.release.ReleaseStatus;
import dev.nthings.helm4j.release.ReleaseSuccess;
import dev.nthings.helm4j.release.RollbackRequest;
import dev.nthings.helm4j.release.RollbackResult;
import dev.nthings.helm4j.release.RollbackSuccess;
import dev.nthings.helm4j.release.StatusRequest;
import dev.nthings.helm4j.release.StatusResult;
import dev.nthings.helm4j.release.TestHookResult;
import dev.nthings.helm4j.release.TestRequest;
import dev.nthings.helm4j.release.TestResult;
import dev.nthings.helm4j.release.UninstallRequest;
import dev.nthings.helm4j.release.UninstallResult;
import dev.nthings.helm4j.release.UninstallSuccess;
import dev.nthings.helm4j.release.UpgradeRequest;
import dev.nthings.helm4j.release.UpgradeResult;
import dev.nthings.helm4j.repo.RegistryLoginRequest;
import dev.nthings.helm4j.repo.RegistryLogoutRequest;
import dev.nthings.helm4j.repo.RegistryResult;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Gateway backed by JSON bridge functions exported by libhelm4j. */
public final class NativeStructGateway implements HelmGateway {

  private static final Logger log = LoggerFactory.getLogger(NativeStructGateway.class);

  private final HelmBridge bridge;
  private final ObjectMapper mapper;

  public NativeStructGateway(HelmBridge bridge, ObjectMapper mapper) {
    this.bridge = Objects.requireNonNull(bridge, "bridge");
    this.mapper = Objects.requireNonNull(mapper, "mapper");
  }

  @Override
  public RepoAddResult repoAdd(RepoAddRequest request) {
    Objects.requireNonNull(request, "request");
    if (request.name() == null || request.url() == null) {
      throw new IllegalArgumentException("Repository add requires non-null name and url");
    }

    log.debug("Adding repository: name={}, url={}", request.name(), request.url());
    var root =
        invokeRoot(
            "repo add",
            () ->
                bridge.repo(utf8("add"), toJsonBytes(NativeOptions.repoAdd(request), "repo add")));

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

    log.debug("Updating repositories: names={}", request.names());
    var root =
        invokeRootOrThrow(
            "repo update",
            () ->
                bridge.repo(
                    utf8("update"), toJsonBytes(NativeOptions.repoUpdate(request), "repo update")));

    var response = convert(root, RepoUpdatePayload.class, "repo update");
    var repositories =
        listOrEmpty(response == null ? null : response.repositories()).stream()
            .map(entry -> new RepoUpdateEntry(entry.name(), entry.status()))
            .toList();
    return new RepoUpdateResult(repositories);
  }

  @Override
  public RepoListResult repoList() {
    log.debug("Listing repositories");
    var root =
        invokeRootOrThrow(
            "repo list", () -> bridge.repo(utf8("list"), toJsonBytes(Map.of(), "repo list")));

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

    log.debug("Removing repositories: names={}", request.names());
    var root =
        invokeRootOrThrow(
            "repo remove",
            () ->
                bridge.repo(
                    utf8("remove"), toJsonBytes(NativeOptions.repoRemove(request), "repo remove")));

    var response = convert(root, RepoRemovePayload.class, "repo remove");
    return new RepoRemoveResult(listOrEmpty(response == null ? null : response.removed()));
  }

  @Override
  public RepoSearchResult searchRepo(RepoSearchRequest request) {
    Objects.requireNonNull(request, "request");

    log.debug("Searching repositories: keyword={}", request.keyword());
    var root =
        invokeRootOrThrow(
            "search repo",
            () ->
                bridge.search(
                    utf8("repo"), toJsonBytes(NativeOptions.searchRepo(request), "search repo")));

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

    log.debug("Searching hub: keyword={}", request.keyword());
    var root =
        invokeRootOrThrow(
            "search hub",
            () ->
                bridge.search(
                    utf8("hub"), toJsonBytes(NativeOptions.searchHub(request), "search hub")));

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
  public PullResult pull(PullRequest request) {
    Objects.requireNonNull(request, "request");
    if (request.chartReference() == null) {
      throw new IllegalArgumentException("Pull requires chart reference");
    }

    log.debug("Pulling chart: chartRef={}", request.chartReference());
    var root =
        invokeRootOrThrow(
            "pull",
            () ->
                bridge.pull(
                    utf8(request.chartReference()),
                    toJsonBytes(NativeOptions.pull(request), "pull")));

    var response = convert(root, PullPayload.class, "pull");
    return new PullResult(response == null ? "" : response.output());
  }

  @Override
  public PushResult push(PushRequest request) {
    Objects.requireNonNull(request, "request");
    if (request.chartReference() == null || request.remote() == null) {
      throw new IllegalArgumentException("Push requires chart reference and remote");
    }

    log.debug("Pushing chart: chartRef={}, remote={}", request.chartReference(), request.remote());
    var root =
        invokeRootOrThrow(
            "push",
            () ->
                bridge.push(
                    utf8(request.chartReference()),
                    utf8(request.remote()),
                    toJsonBytes(NativeOptions.push(request), "push")));

    var response = convert(root, PushPayload.class, "push");
    return new PushResult(response == null ? "" : response.output());
  }

  @Override
  public PackageChartResult packageChart(PackageChartRequest request) {
    Objects.requireNonNull(request, "request");
    if (request.chartPath() == null) {
      throw new IllegalArgumentException("Package requires chart path");
    }

    log.debug("Packaging chart: chartPath={}", request.chartPath());
    var root =
        invokeRootOrThrow(
            "package",
            () ->
                bridge.packageChart(
                    utf8(request.chartPath().toString()),
                    toJsonBytes(NativeOptions.packageChart(request), "package")));

    var response = convert(root, PackagePayload.class, "package");
    return new PackageChartResult(response == null ? null : response.path());
  }

  @Override
  public DependencyResult dependency(DependencyRequest request) {
    Objects.requireNonNull(request, "request");
    if (request.chartPath() == null) {
      throw new IllegalArgumentException("Dependency operation requires chart path");
    }

    log.debug("Listing chart dependencies: chartPath={}", request.chartPath());
    var root =
        invokeRootOrThrow(
            "dependency",
            () ->
                bridge.dependency(
                    utf8(request.chartPath().toString()),
                    toJsonBytes(NativeOptions.dependency(request), "dependency")));

    var response = convert(root, DependencyPayload.class, "dependency");
    return new DependencyResult(response == null ? "" : response.output());
  }

  @Override
  public ShowResult show(ShowMode mode, ChartRef chartReference, ShowRequest request) {
    var response = runShow(mode, chartReference, request);
    var sections = response.sections();
    return new ShowResult(
        mode,
        response.chartRef(),
        response.chartPath(),
        sections.chart(),
        sections.values(),
        sections.readme(),
        listOrEmpty(sections.crds()),
        response.cliOutput());
  }

  @Override
  public InstallResult install(InstallRequest request) {
    Objects.requireNonNull(request, "request");
    if (request.chart() == null) {
      throw new IllegalArgumentException("Install requires chart reference");
    }

    log.debug(
        "Installing release: name={}, chart={}",
        request.releaseName(),
        request.chart().asReference());
    var root =
        invokeRoot(
            "install",
            () ->
                bridge.install(
                    utf8(request.releaseName()),
                    utf8(request.chart().asReference()),
                    toJsonBytes(NativeOptions.install(request), "install")));

    var failure = operationError(root, "install");
    if (failure != null) {
      return new ReleaseFailure(
          messageOrUnknown(failure.message()), failure.stage(), failure.operation());
    }

    var response = convert(root, InstallPayload.class, "install");
    if (response == null || response.release() == null) {
      throw new HelmException(
          "Native install response missing release", "decodeResponse", "install");
    }

    var release = mapReleasePayload(response.release(), "install");

    if (release.status().isPending()) {
      return new ReleasePending(release);
    }
    return new ReleaseSuccess(release);
  }

  @Override
  public UpgradeResult upgrade(UpgradeRequest request) {
    Objects.requireNonNull(request, "request");
    if (request.chart() == null) {
      throw new IllegalArgumentException("Upgrade requires chart reference");
    }

    log.debug(
        "Upgrading release: name={}, chart={}",
        request.releaseName(),
        request.chart().asReference());
    var root =
        invokeRoot(
            "upgrade",
            () ->
                bridge.upgrade(
                    utf8(request.releaseName()),
                    utf8(request.chart().asReference()),
                    toJsonBytes(NativeOptions.upgrade(request), "upgrade")));

    var failure = operationError(root, "upgrade");
    if (failure != null) {
      return new ReleaseFailure(
          messageOrUnknown(failure.message()), failure.stage(), failure.operation());
    }

    var response = convert(root, ReleasePayload.class, "upgrade");
    if (response == null || response.release() == null) {
      throw new HelmException(
          "Native upgrade response missing release", "decodeResponse", "upgrade");
    }

    var release = mapReleasePayload(response.release(), "upgrade");
    if (release.status().isPending()) {
      return new ReleasePending(release);
    }
    return new ReleaseSuccess(release);
  }

  @Override
  public UninstallResult uninstall(UninstallRequest request) {
    Objects.requireNonNull(request, "request");

    log.debug("Uninstalling release: name={}", request.releaseName());
    var root =
        invokeRoot(
            "uninstall",
            () ->
                bridge.uninstall(
                    utf8(request.releaseName()),
                    toJsonBytes(NativeOptions.uninstall(request), "uninstall")));

    var failure = operationError(root, "uninstall");
    if (failure != null) {
      return new ReleaseFailure(
          messageOrUnknown(failure.message()), failure.stage(), failure.operation());
    }

    var response = convert(root, UninstallPayload.class, "uninstall");
    if (response == null) {
      throw new HelmException(
          "Native uninstall response missing data", "decodeResponse", "uninstall");
    }

    var release =
        response.release() != null ? mapReleasePayload(response.release(), "uninstall") : null;
    return new UninstallSuccess(release, response.info());
  }

  @Override
  public StatusResult status(StatusRequest request) {
    Objects.requireNonNull(request, "request");

    log.debug("Getting status: name={}", request.releaseName());
    var root =
        invokeRootOrThrow(
            "status",
            () ->
                bridge.status(
                    utf8(request.releaseName()),
                    toJsonBytes(NativeOptions.status(request), "status")));

    var response = convert(root, ReleasePayload.class, "status");
    if (response == null || response.release() == null) {
      throw new HelmException("Native status response missing release", "decodeResponse", "status");
    }

    return new StatusResult(mapReleasePayload(response.release(), "status"));
  }

  @Override
  public RollbackResult rollback(RollbackRequest request) {
    Objects.requireNonNull(request, "request");

    log.debug("Rolling back release: name={}", request.releaseName());
    var root =
        invokeRoot(
            "rollback",
            () ->
                bridge.rollback(
                    utf8(request.releaseName()),
                    toJsonBytes(NativeOptions.rollback(request), "rollback")));

    var failure = operationError(root, "rollback");
    if (failure != null) {
      return new ReleaseFailure(
          messageOrUnknown(failure.message()), failure.stage(), failure.operation());
    }

    var response = convert(root, RollbackPayload.class, "rollback");
    if (response == null) {
      throw new HelmException(
          "Native rollback response missing data", "decodeResponse", "rollback");
    }

    return new RollbackSuccess(response.releaseName(), response.revision());
  }

  @Override
  public HistoryResult history(HistoryRequest request) {
    Objects.requireNonNull(request, "request");

    log.debug("Getting history: name={}", request.releaseName());
    var root =
        invokeRootOrThrow(
            "history",
            () ->
                bridge.history(
                    utf8(request.releaseName()),
                    toJsonBytes(NativeOptions.history(request), "history")));

    var response = convert(root, HistoryPayload.class, "history");
    var entries =
        listOrEmpty(response == null ? null : response.entries()).stream()
            .map(
                e ->
                    new HistoryEntry(
                        e.revision(),
                        parseTimestamp(e.updated(), "history", "updated"),
                        ReleaseStatus.fromWireValue(e.status()),
                        e.chart(),
                        e.chartVersion(),
                        e.appVersion(),
                        e.description()))
            .toList();
    return new HistoryResult(entries);
  }

  @Override
  public ReleaseListResult list(ReleaseListRequest request) {
    Objects.requireNonNull(request, "request");

    log.debug(
        "Listing releases: namespace={}, allNamespaces={}",
        request.namespace(),
        request.allNamespaces());
    var root =
        invokeRootOrThrow(
            "list", () -> bridge.list(toJsonBytes(NativeOptions.list(request), "list")));

    var response = convert(root, ListPayload.class, "list");
    var releases =
        listOrEmpty(response == null ? null : response.releases()).stream()
            .map(release -> mapReleasePayload(release, "list"))
            .toList();
    return new ReleaseListResult(releases);
  }

  @Override
  public TestResult test(TestRequest request) {
    Objects.requireNonNull(request, "request");
    if (request.releaseName() == null) {
      throw new IllegalArgumentException("Test requires release name");
    }

    log.debug("Testing release: name={}", request.releaseName());
    var root =
        invokeRootOrThrow(
            "test",
            () ->
                bridge.test(
                    utf8(request.releaseName()), toJsonBytes(NativeOptions.test(request), "test")));

    var response = convert(root, TestPayload.class, "test");
    if (response == null || response.release() == null) {
      throw new HelmException("Native test response missing release", "decodeResponse", "test");
    }

    var results =
        listOrEmpty(response.results()).stream()
            .map(r -> new TestHookResult(r.name(), r.status()))
            .toList();
    return new TestResult(mapReleasePayload(response.release(), "test"), results);
  }

  @Override
  public GetAllResult getAll(GetRequest request) {
    Objects.requireNonNull(request, "request");
    var response = convert(runGetRoot(GetMode.ALL, request), GetAllPayload.class, "get all");
    if (response == null || response.release() == null) {
      throw new HelmException(
          "Native get all response missing release", "decodeResponse", "get all");
    }
    return new GetAllResult(
        mapReleasePayload(response.release(), "get all"),
        mapOrEmpty(response.values()),
        response.manifest(),
        mapHooks(response.hooks()),
        response.notes());
  }

  @Override
  public GetValuesResult getValues(GetRequest request) {
    Objects.requireNonNull(request, "request");
    var response =
        convert(runGetRoot(GetMode.VALUES, request), GetValuesPayload.class, "get values");
    return new GetValuesResult(mapOrEmpty(response == null ? null : response.values()));
  }

  @Override
  public GetManifestResult getManifest(GetRequest request) {
    Objects.requireNonNull(request, "request");
    var response =
        convert(runGetRoot(GetMode.MANIFEST, request), GetManifestPayload.class, "get manifest");
    return new GetManifestResult(response == null ? "" : response.manifest());
  }

  @Override
  public GetHooksResult getHooks(GetRequest request) {
    Objects.requireNonNull(request, "request");
    var response = convert(runGetRoot(GetMode.HOOKS, request), GetHooksPayload.class, "get hooks");
    return new GetHooksResult(mapHooks(response == null ? null : response.hooks()));
  }

  @Override
  public GetNotesResult getNotes(GetRequest request) {
    Objects.requireNonNull(request, "request");
    var response = convert(runGetRoot(GetMode.NOTES, request), GetNotesPayload.class, "get notes");
    return new GetNotesResult(response == null ? "" : response.notes());
  }

  @Override
  public GetMetadataResult getMetadata(GetRequest request) {
    Objects.requireNonNull(request, "request");
    var response =
        convert(runGetRoot(GetMode.METADATA, request), GetMetadataPayload.class, "get metadata");
    if (response == null) {
      throw new HelmException(
          "Native get metadata response missing data", "decodeResponse", "get metadata");
    }
    return new GetMetadataResult(
        response.name(),
        response.namespace(),
        response.revision(),
        ReleaseStatus.fromWireValue(response.status()),
        response.chart(),
        response.chartVersion(),
        response.appVersion(),
        parseTimestamp(response.deployedAt(), "get metadata", "deployedAt"));
  }

  @Override
  public TemplateResult template(TemplateRequest request) {
    Objects.requireNonNull(request, "request");
    if (request.chart() == null) {
      throw new IllegalArgumentException("Template requires chart reference");
    }

    log.debug(
        "Templating chart: name={}, chart={}",
        request.releaseName(),
        request.chart().asReference());
    var root =
        invokeRootOrThrow(
            "template",
            () ->
                bridge.template(
                    utf8(request.releaseName()),
                    utf8(request.chart().asReference()),
                    toJsonBytes(NativeOptions.template(request), "template")));

    var response = convert(root, TemplatePayload.class, "template");
    if (response == null || response.release() == null) {
      throw new HelmException(
          "Native template response missing release", "decodeResponse", "template");
    }

    return new TemplateResult(
        mapReleasePayload(response.release(), "template"), response.manifest());
  }

  @Override
  public LintResult lint(LintRequest request) {
    Objects.requireNonNull(request, "request");

    log.debug("Linting chart: path={}", request.chartPath());
    var root =
        invokeRootOrThrow(
            "lint",
            () ->
                bridge.lint(
                    utf8(request.chartPath().toString()),
                    toJsonBytes(NativeOptions.lint(request), "lint")));

    var response = convert(root, LintPayload.class, "lint");
    var messages =
        listOrEmpty(response == null ? null : response.messages()).stream()
            .map(m -> new LintMessage(LintSeverity.fromWireValue(m.severity()), m.message()))
            .toList();
    return new LintResult(
        messages,
        response == null ? 0 : response.totalCharts(),
        response == null ? 0 : response.chartsTested(),
        response == null ? 0 : response.chartsFailed());
  }

  @Override
  public RegistryResult registryLogin(RegistryLoginRequest request) {
    Objects.requireNonNull(request, "request");
    if (request.hostname() == null) {
      throw new IllegalArgumentException("Registry login requires hostname");
    }

    log.debug("Registry login: hostname={}", request.hostname());
    var root =
        invokeRootOrThrow(
            "registry login",
            () ->
                bridge.registry(
                    utf8("login"),
                    utf8(request.hostname()),
                    toJsonBytes(NativeOptions.registryLogin(request), "registry login")));

    var response = convert(root, RegistryPayload.class, "registry login");
    if (response == null) {
      throw new HelmException(
          "Native registry login response missing data", "decodeResponse", "registry login");
    }
    return new RegistryResult(response.hostname(), response.status());
  }

  @Override
  public RegistryResult registryLogout(RegistryLogoutRequest request) {
    Objects.requireNonNull(request, "request");
    if (request.hostname() == null) {
      throw new IllegalArgumentException("Registry logout requires hostname");
    }

    log.debug("Registry logout: hostname={}", request.hostname());
    var root =
        invokeRootOrThrow(
            "registry logout",
            () ->
                bridge.registry(
                    utf8("logout"),
                    utf8(request.hostname()),
                    toJsonBytes(Map.of(), "registry logout")));

    var response = convert(root, RegistryPayload.class, "registry logout");
    if (response == null) {
      throw new HelmException(
          "Native registry logout response missing data", "decodeResponse", "registry logout");
    }
    return new RegistryResult(response.hostname(), response.status());
  }

  @Override
  public VersionInfo version() {
    log.debug("Getting version info");
    var root = invokeRootOrThrow("version", bridge::version);

    var response = convert(root, VersionPayload.class, "version");
    if (response == null) {
      throw new HelmException("Native version response missing data", "decodeResponse", "version");
    }
    return new VersionInfo(response.version(), response.goVersion(), response.helmVersion());
  }

  private ShowPayload runShow(ShowMode mode, ChartRef chartReference, ShowRequest request) {
    Objects.requireNonNull(mode, "mode");
    Objects.requireNonNull(chartReference, "chartReference");
    Objects.requireNonNull(request, "request");

    var operation = "show " + mode.wireValue();
    log.debug("Show operation: mode={}, chart={}", mode.wireValue(), chartReference.asReference());
    var root =
        invokeRootOrThrow(
            operation,
            () ->
                bridge.show(
                    utf8(mode.wireValue()),
                    utf8(chartReference.asReference()),
                    toJsonBytes(NativeOptions.show(request), operation)));

    var response = convert(root, ShowPayload.class, operation);
    if (response == null || response.sections() == null) {
      throw new HelmException("Native show response missing sections", "decodeResponse", operation);
    }

    if (response.mode() != null && !mode.wireValue().equals(response.mode())) {
      throw new HelmException("Native show response mode mismatch", "decodeResponse", operation);
    }

    return response;
  }

  private byte[] invoke(ByteArrayInvocation invocation, String operation, String stage) {
    final byte[] payload;
    try {
      payload = invocation.invoke();
    } catch (RuntimeException error) {
      throw new HelmException("Native bridge invocation failed", stage, operation, error);
    }

    if (payload == null || payload.length == 0) {
      throw new HelmException("Native bridge returned empty response", stage, operation);
    }
    return payload;
  }

  private JsonNode invokeRoot(String operation, ByteArrayInvocation invocation) {
    return parse(invoke(invocation, operation, "invokeNative"), operation);
  }

  private JsonNode invokeRootOrThrow(String operation, ByteArrayInvocation invocation) {
    var root = invokeRoot(operation, invocation);
    var failure = operationError(root, operation);
    if (failure != null) {
      throw asException(failure);
    }
    return root;
  }

  private JsonNode parse(byte[] payload, String operation) {
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
    return value.asString();
  }

  private byte[] toJsonBytes(Map<String, Object> payload, String operation) {
    try {
      return mapper.writeValueAsBytes(payload);
    } catch (JacksonException error) {
      throw new HelmException("Failed to encode native options", "encodeOptions", operation, error);
    }
  }

  private static byte[] utf8(String value) {
    if (value == null) {
      return null;
    }
    return value.getBytes(StandardCharsets.UTF_8);
  }

  private JsonNode runGetRoot(GetMode mode, GetRequest request) {
    var operation = "get " + mode.wireValue();
    log.debug("Get operation: mode={}, release={}", mode.wireValue(), request.releaseName());
    return invokeRootOrThrow(
        operation,
        () ->
            bridge.get(
                utf8(mode.wireValue()),
                utf8(request.releaseName()),
                toJsonBytes(NativeOptions.get(request), operation)));
  }

  private static ReleaseInfo mapReleasePayload(NativeReleasePayload r, String operation) {
    return new ReleaseInfo(
        r.name(),
        r.namespace(),
        r.revision(),
        ReleaseStatus.fromWireValue(r.status()),
        r.description(),
        parseTimestamp(r.firstDeployed(), operation, "firstDeployed"),
        parseTimestamp(r.lastDeployed(), operation, "lastDeployed"),
        r.chartName(),
        r.chartVersion(),
        r.appVersion(),
        r.notes());
  }

  private static List<HookInfo> mapHooks(List<HookPayload> hooks) {
    return listOrEmpty(hooks).stream()
        .map(h -> new HookInfo(h.name(), h.kind(), h.path(), listOrEmpty(h.events()), h.weight()))
        .toList();
  }

  private static Map<String, Object> mapOrEmpty(Map<String, Object> value) {
    return value == null ? Map.of() : value;
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

  private static Instant parseTimestamp(String value, String operation, String field) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Instant.parse(value);
    } catch (DateTimeParseException error) {
      throw new HelmException(
          "Invalid timestamp for " + field + ": " + value, "decodeResponse", operation, error);
    }
  }

  private static <T> List<T> listOrEmpty(List<T> value) {
    return value == null ? List.of() : value;
  }

  @FunctionalInterface
  private interface ByteArrayInvocation {
    byte[] invoke();
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

  private record InstallPayload(NativeReleasePayload release) {}

  private record ReleasePayload(NativeReleasePayload release) {}

  private record UninstallPayload(NativeReleasePayload release, String info) {}

  private record RollbackPayload(String releaseName, int revision) {}

  private record HistoryPayload(List<HistoryEntryPayload> entries) {}

  private record HistoryEntryPayload(
      int revision,
      String updated,
      String status,
      String chart,
      String chartVersion,
      String appVersion,
      String description) {}

  private record GetAllPayload(
      NativeReleasePayload release,
      Map<String, Object> values,
      String manifest,
      List<HookPayload> hooks,
      String notes) {}

  private record GetValuesPayload(Map<String, Object> values) {}

  private record GetManifestPayload(String manifest) {}

  private record GetHooksPayload(List<HookPayload> hooks) {}

  private record GetNotesPayload(String notes) {}

  private record GetMetadataPayload(
      String name,
      String namespace,
      int revision,
      String status,
      String chart,
      String chartVersion,
      String appVersion,
      String deployedAt) {}

  private record HookPayload(
      String name, String kind, String path, List<String> events, int weight) {}

  private record TemplatePayload(NativeReleasePayload release, String manifest) {}

  private record LintPayload(
      List<LintMessagePayload> messages, int totalCharts, int chartsTested, int chartsFailed) {}

  private record LintMessagePayload(String severity, String message) {}

  private record PullPayload(String output) {}

  private record PushPayload(String output) {}

  private record PackagePayload(String path) {}

  private record DependencyPayload(String output) {}

  private record ListPayload(List<NativeReleasePayload> releases) {}

  private record TestPayload(NativeReleasePayload release, List<TestHookPayload> results) {}

  private record TestHookPayload(String name, String status) {}

  private record RegistryPayload(String hostname, String status) {}

  private record VersionPayload(String version, String goVersion, String helmVersion) {}

  private record NativeReleasePayload(
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
