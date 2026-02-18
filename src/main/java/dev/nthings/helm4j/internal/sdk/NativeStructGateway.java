package dev.nthings.helm4j.internal.sdk;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import dev.nthings.helm4j.VersionInfo;
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
import dev.nthings.helm4j.chart.ShowAllResult;
import dev.nthings.helm4j.chart.ShowChartResult;
import dev.nthings.helm4j.chart.ShowCrdsResult;
import dev.nthings.helm4j.chart.ShowMode;
import dev.nthings.helm4j.chart.ShowReadmeResult;
import dev.nthings.helm4j.chart.ShowRequest;
import dev.nthings.helm4j.chart.ShowValuesResult;
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
import dev.nthings.helm4j.release.InstallFailure;
import dev.nthings.helm4j.release.InstallPending;
import dev.nthings.helm4j.release.InstallRequest;
import dev.nthings.helm4j.release.InstallResult;
import dev.nthings.helm4j.release.InstallSuccess;
import dev.nthings.helm4j.release.ReleaseInfo;
import dev.nthings.helm4j.release.ReleaseListRequest;
import dev.nthings.helm4j.release.ReleaseListResult;
import dev.nthings.helm4j.release.RollbackFailure;
import dev.nthings.helm4j.release.RollbackRequest;
import dev.nthings.helm4j.release.RollbackResult;
import dev.nthings.helm4j.release.RollbackSuccess;
import dev.nthings.helm4j.release.StatusRequest;
import dev.nthings.helm4j.release.StatusResult;
import dev.nthings.helm4j.release.TestHookResult;
import dev.nthings.helm4j.release.TestRequest;
import dev.nthings.helm4j.release.TestResult;
import dev.nthings.helm4j.release.UninstallFailure;
import dev.nthings.helm4j.release.UninstallRequest;
import dev.nthings.helm4j.release.UninstallResult;
import dev.nthings.helm4j.release.UninstallSuccess;
import dev.nthings.helm4j.release.UpgradeFailure;
import dev.nthings.helm4j.release.UpgradePending;
import dev.nthings.helm4j.release.UpgradeRequest;
import dev.nthings.helm4j.release.UpgradeResult;
import dev.nthings.helm4j.release.UpgradeSuccess;
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
import dev.nthings.helm4j.types.ChartRef;

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
    var payload =
        invoke(
            () -> bridge.repo(utf8("add"), toJsonBytes(repoAddOptions(request), "repo add")),
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

    log.debug("Updating repositories: names={}", request.names());
    var payload =
        invoke(
            () ->
                bridge.repo(utf8("update"), toJsonBytes(repoUpdateOptions(request), "repo update")),
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
    log.debug("Listing repositories");
    var payload =
        invoke(
            () -> bridge.repo(utf8("list"), toJsonBytes(Map.of(), "repo list")),
            "repo list",
            "invokeNative");
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

    log.debug("Removing repositories: names={}", request.names());
    var payload =
        invoke(
            () ->
                bridge.repo(utf8("remove"), toJsonBytes(repoRemoveOptions(request), "repo remove")),
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

    log.debug("Searching repositories: keyword={}", request.keyword());
    var payload =
        invoke(
            () ->
                bridge.search(utf8("repo"), toJsonBytes(searchRepoOptions(request), "search repo")),
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

    log.debug("Searching hub: keyword={}", request.keyword());
    var payload =
        invoke(
            () -> bridge.search(utf8("hub"), toJsonBytes(searchHubOptions(request), "search hub")),
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
  public PullResult pull(PullRequest request) {
    Objects.requireNonNull(request, "request");
    if (request.chartReference() == null) {
      throw new IllegalArgumentException("Pull requires chart reference");
    }

    log.debug("Pulling chart: chartRef={}", request.chartReference());
    var payload =
        invoke(
            () ->
                bridge.pull(
                    utf8(request.chartReference()), toJsonBytes(pullOptions(request), "pull")),
            "pull",
            "invokeNative");
    var root = parse(payload, "pull");

    var failure = operationError(root, "pull");
    if (failure != null) {
      throw asException(failure);
    }

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
    var payload =
        invoke(
            () ->
                bridge.push(
                    utf8(request.chartReference()),
                    utf8(request.remote()),
                    toJsonBytes(pushOptions(request), "push")),
            "push",
            "invokeNative");
    var root = parse(payload, "push");

    var failure = operationError(root, "push");
    if (failure != null) {
      throw asException(failure);
    }

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
    var payload =
        invoke(
            () ->
                bridge.packageChart(
                    utf8(request.chartPath().toString()),
                    toJsonBytes(packageOptions(request), "package")),
            "package",
            "invokeNative");
    var root = parse(payload, "package");

    var failure = operationError(root, "package");
    if (failure != null) {
      throw asException(failure);
    }

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
    var payload =
        invoke(
            () ->
                bridge.dependency(
                    utf8(request.chartPath().toString()),
                    toJsonBytes(dependencyOptions(request), "dependency")),
            "dependency",
            "invokeNative");
    var root = parse(payload, "dependency");

    var failure = operationError(root, "dependency");
    if (failure != null) {
      throw asException(failure);
    }

    var response = convert(root, DependencyPayload.class, "dependency");
    return new DependencyResult(response == null ? "" : response.output());
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

    log.debug(
        "Installing release: name={}, chart={}",
        request.releaseName(),
        request.chart().asReference());
    var payload =
        invoke(
            () ->
                bridge.install(
                    utf8(request.releaseName()),
                    utf8(request.chart().asReference()),
                    toJsonBytes(installOptions(request), "install")),
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

    var release = mapReleasePayload(response.release());

    if (isPendingStatus(release.status())) {
      return new InstallPending(release);
    }
    return new InstallSuccess(release);
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
    var payload =
        invoke(
            () ->
                bridge.upgrade(
                    utf8(request.releaseName()),
                    utf8(request.chart().asReference()),
                    toJsonBytes(upgradeOptions(request), "upgrade")),
            "upgrade",
            "invokeNative");
    var root = parse(payload, "upgrade");

    var failure = operationError(root, "upgrade");
    if (failure != null) {
      return new UpgradeFailure(
          messageOrUnknown(failure.message()), failure.stage(), failure.operation());
    }

    var response = convert(root, ReleasePayload.class, "upgrade");
    if (response == null || response.release() == null) {
      throw new HelmException(
          "Native upgrade response missing release", "decodeResponse", "upgrade");
    }

    var release = mapReleasePayload(response.release());
    if (isPendingStatus(release.status())) {
      return new UpgradePending(release);
    }
    return new UpgradeSuccess(release);
  }

  @Override
  public UninstallResult uninstall(UninstallRequest request) {
    Objects.requireNonNull(request, "request");

    log.debug("Uninstalling release: name={}", request.releaseName());
    var payload =
        invoke(
            () ->
                bridge.uninstall(
                    utf8(request.releaseName()),
                    toJsonBytes(uninstallOptions(request), "uninstall")),
            "uninstall",
            "invokeNative");
    var root = parse(payload, "uninstall");

    var failure = operationError(root, "uninstall");
    if (failure != null) {
      return new UninstallFailure(
          messageOrUnknown(failure.message()), failure.stage(), failure.operation());
    }

    var response = convert(root, UninstallPayload.class, "uninstall");
    if (response == null) {
      throw new HelmException(
          "Native uninstall response missing data", "decodeResponse", "uninstall");
    }

    var release = response.release() != null ? mapReleasePayload(response.release()) : null;
    return new UninstallSuccess(release, response.info());
  }

  @Override
  public StatusResult status(StatusRequest request) {
    Objects.requireNonNull(request, "request");

    log.debug("Getting status: name={}", request.releaseName());
    var payload =
        invoke(
            () ->
                bridge.status(
                    utf8(request.releaseName()), toJsonBytes(statusOptions(request), "status")),
            "status",
            "invokeNative");
    var root = parse(payload, "status");

    var failure = operationError(root, "status");
    if (failure != null) {
      throw asException(failure);
    }

    var response = convert(root, ReleasePayload.class, "status");
    if (response == null || response.release() == null) {
      throw new HelmException("Native status response missing release", "decodeResponse", "status");
    }

    return new StatusResult(mapReleasePayload(response.release()));
  }

  @Override
  public RollbackResult rollback(RollbackRequest request) {
    Objects.requireNonNull(request, "request");

    log.debug("Rolling back release: name={}", request.releaseName());
    var payload =
        invoke(
            () ->
                bridge.rollback(
                    utf8(request.releaseName()), toJsonBytes(rollbackOptions(request), "rollback")),
            "rollback",
            "invokeNative");
    var root = parse(payload, "rollback");

    var failure = operationError(root, "rollback");
    if (failure != null) {
      return new RollbackFailure(
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
    var payload =
        invoke(
            () ->
                bridge.history(
                    utf8(request.releaseName()), toJsonBytes(historyOptions(request), "history")),
            "history",
            "invokeNative");
    var root = parse(payload, "history");

    var failure = operationError(root, "history");
    if (failure != null) {
      throw asException(failure);
    }

    var response = convert(root, HistoryPayload.class, "history");
    var entries =
        listOrEmpty(response == null ? null : response.entries()).stream()
            .map(
                e ->
                    new HistoryEntry(
                        e.revision(),
                        e.updated(),
                        e.status(),
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
    var payload =
        invoke(
            () -> bridge.list(toJsonBytes(listOptions(request), "list")), "list", "invokeNative");
    var root = parse(payload, "list");

    var failure = operationError(root, "list");
    if (failure != null) {
      throw asException(failure);
    }

    var response = convert(root, ListPayload.class, "list");
    var releases =
        listOrEmpty(response == null ? null : response.releases()).stream()
            .map(NativeStructGateway::mapReleasePayload)
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
    var payload =
        invoke(
            () ->
                bridge.test(utf8(request.releaseName()), toJsonBytes(testOptions(request), "test")),
            "test",
            "invokeNative");
    var root = parse(payload, "test");

    var failure = operationError(root, "test");
    if (failure != null) {
      throw asException(failure);
    }

    var response = convert(root, TestPayload.class, "test");
    if (response == null || response.release() == null) {
      throw new HelmException("Native test response missing release", "decodeResponse", "test");
    }

    var results =
        listOrEmpty(response.results()).stream()
            .map(r -> new TestHookResult(r.name(), r.status()))
            .toList();
    return new TestResult(mapReleasePayload(response.release()), results);
  }

  @Override
  public GetAllResult getAll(GetRequest request) {
    Objects.requireNonNull(request, "request");
    var payload = runGet(GetMode.ALL, request);
    var response = convert(parse(payload, "get all"), GetAllPayload.class, "get all");
    if (response == null || response.release() == null) {
      throw new HelmException(
          "Native get all response missing release", "decodeResponse", "get all");
    }
    return new GetAllResult(
        mapReleasePayload(response.release()),
        mapOrEmpty(response.values()),
        response.manifest(),
        mapHooks(response.hooks()),
        response.notes());
  }

  @Override
  public GetValuesResult getValues(GetRequest request) {
    Objects.requireNonNull(request, "request");
    var payload = runGet(GetMode.VALUES, request);
    var response = convert(parse(payload, "get values"), GetValuesPayload.class, "get values");
    return new GetValuesResult(mapOrEmpty(response == null ? null : response.values()));
  }

  @Override
  public GetManifestResult getManifest(GetRequest request) {
    Objects.requireNonNull(request, "request");
    var payload = runGet(GetMode.MANIFEST, request);
    var response =
        convert(parse(payload, "get manifest"), GetManifestPayload.class, "get manifest");
    return new GetManifestResult(response == null ? "" : response.manifest());
  }

  @Override
  public GetHooksResult getHooks(GetRequest request) {
    Objects.requireNonNull(request, "request");
    var payload = runGet(GetMode.HOOKS, request);
    var response = convert(parse(payload, "get hooks"), GetHooksPayload.class, "get hooks");
    return new GetHooksResult(mapHooks(response == null ? null : response.hooks()));
  }

  @Override
  public GetNotesResult getNotes(GetRequest request) {
    Objects.requireNonNull(request, "request");
    var payload = runGet(GetMode.NOTES, request);
    var response = convert(parse(payload, "get notes"), GetNotesPayload.class, "get notes");
    return new GetNotesResult(response == null ? "" : response.notes());
  }

  @Override
  public GetMetadataResult getMetadata(GetRequest request) {
    Objects.requireNonNull(request, "request");
    var payload = runGet(GetMode.METADATA, request);
    var response =
        convert(parse(payload, "get metadata"), GetMetadataPayload.class, "get metadata");
    if (response == null) {
      throw new HelmException(
          "Native get metadata response missing data", "decodeResponse", "get metadata");
    }
    return new GetMetadataResult(
        response.name(),
        response.namespace(),
        response.revision(),
        response.status(),
        response.chart(),
        response.chartVersion(),
        response.appVersion(),
        response.deployedAt());
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
    var payload =
        invoke(
            () ->
                bridge.template(
                    utf8(request.releaseName()),
                    utf8(request.chart().asReference()),
                    toJsonBytes(templateOptions(request), "template")),
            "template",
            "invokeNative");
    var root = parse(payload, "template");

    var failure = operationError(root, "template");
    if (failure != null) {
      throw asException(failure);
    }

    var response = convert(root, TemplatePayload.class, "template");
    if (response == null || response.release() == null) {
      throw new HelmException(
          "Native template response missing release", "decodeResponse", "template");
    }

    return new TemplateResult(mapReleasePayload(response.release()), response.manifest());
  }

  @Override
  public LintResult lint(LintRequest request) {
    Objects.requireNonNull(request, "request");

    log.debug("Linting chart: path={}", request.chartPath());
    var payload =
        invoke(
            () ->
                bridge.lint(
                    utf8(request.chartPath().toString()),
                    toJsonBytes(lintOptions(request), "lint")),
            "lint",
            "invokeNative");
    var root = parse(payload, "lint");

    var failure = operationError(root, "lint");
    if (failure != null) {
      throw asException(failure);
    }

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
    var payload =
        invoke(
            () ->
                bridge.registry(
                    utf8("login"),
                    utf8(request.hostname()),
                    toJsonBytes(registryLoginOptions(request), "registry login")),
            "registry login",
            "invokeNative");
    var root = parse(payload, "registry login");

    var failure = operationError(root, "registry login");
    if (failure != null) {
      throw asException(failure);
    }

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
    var payload =
        invoke(
            () ->
                bridge.registry(
                    utf8("logout"),
                    utf8(request.hostname()),
                    toJsonBytes(Map.of(), "registry logout")),
            "registry logout",
            "invokeNative");
    var root = parse(payload, "registry logout");

    var failure = operationError(root, "registry logout");
    if (failure != null) {
      throw asException(failure);
    }

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
    var payload = invoke(bridge::version, "version", "invokeNative");
    var root = parse(payload, "version");

    var failure = operationError(root, "version");
    if (failure != null) {
      throw asException(failure);
    }

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
    var payload =
        invoke(
            () ->
                bridge.show(
                    utf8(mode.wireValue()),
                    utf8(chartReference.asReference()),
                    toJsonBytes(showOptions(request), operation)),
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
    return value.asText();
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

  private static Map<String, Object> upgradeOptions(UpgradeRequest request) {
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
    options.put("install", request.install());
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
    options.put("subNotes", request.subNotes());
    options.put("enableDns", request.enableDns());
    options.put("takeOwnership", request.takeOwnership());
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

  private static Map<String, Object> uninstallOptions(UninstallRequest request) {
    var options = new LinkedHashMap<String, Object>();
    putIfNonNull(options, "namespace", request.namespace());
    options.put("dryRun", request.dryRun());
    options.put("disableHooks", request.disableHooks());
    options.put("keepHistory", request.keepHistory());
    options.put("ignoreNotFound", request.ignoreNotFound());
    putIfNonNull(options, "timeout", durationString(request.timeout()));
    putIfNonNull(options, "description", request.description());
    putIfNonNull(
        options, "wait", request.waitMode() == null ? null : request.waitMode().wireValue());
    putIfNonNull(options, "deletionPropagation", request.deletionPropagation());
    return options;
  }

  private static Map<String, Object> statusOptions(StatusRequest request) {
    var options = new LinkedHashMap<String, Object>();
    putIfNonNull(options, "namespace", request.namespace());
    options.put("revision", request.revision());
    return options;
  }

  private static Map<String, Object> rollbackOptions(RollbackRequest request) {
    var options = new LinkedHashMap<String, Object>();
    putIfNonNull(options, "namespace", request.namespace());
    options.put("revision", request.revision());
    putIfNonNull(
        options, "dryRun", request.dryRunMode() == null ? null : request.dryRunMode().wireValue());
    options.put("disableHooks", request.disableHooks());
    options.put("forceReplace", request.forceReplace());
    putIfNonNull(options, "timeout", durationString(request.timeout()));
    putIfNonNull(
        options, "wait", request.waitMode() == null ? null : request.waitMode().wireValue());
    options.put("waitForJobs", request.waitForJobs());
    options.put("cleanupOnFail", request.cleanupOnFail());
    options.put("maxHistory", request.maxHistory());
    options.put("forceConflicts", request.applyStrategy().forceConflicts());
    options.put("serverSideApply", request.applyStrategy().serverSideApply());
    return options;
  }

  private static Map<String, Object> historyOptions(HistoryRequest request) {
    var options = new LinkedHashMap<String, Object>();
    putIfNonNull(options, "namespace", request.namespace());
    options.put("max", request.max());
    return options;
  }

  private static Map<String, Object> getOptions(GetRequest request) {
    var options = new LinkedHashMap<String, Object>();
    putIfNonNull(options, "namespace", request.namespace());
    options.put("revision", request.revision());
    options.put("allValues", request.allValues());
    return options;
  }

  private static Map<String, Object> templateOptions(TemplateRequest request) {
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

  private static Map<String, Object> lintOptions(LintRequest request) {
    var options = new LinkedHashMap<String, Object>();
    options.put("strict", request.strict());
    options.put("quiet", request.quiet());
    options.put("withSubcharts", request.withSubcharts());
    if (!request.values().isEmpty()) {
      options.put("values", request.values());
    }
    return options;
  }

  private static Map<String, Object> pullOptions(PullRequest request) {
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

  private static Map<String, Object> pushOptions(PushRequest request) {
    var options = new LinkedHashMap<String, Object>();
    options.put("plainHttp", request.plainHttp());
    options.put("insecureSkipTlsVerify", request.insecureSkipTlsVerification());
    putIfNonNull(options, "certFile", request.certificateFile());
    putIfNonNull(options, "keyFile", request.keyFile());
    putIfNonNull(options, "caFile", request.certificateAuthorityFile());
    return options;
  }

  private static Map<String, Object> packageOptions(PackageChartRequest request) {
    var options = new LinkedHashMap<String, Object>();
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

  private static Map<String, Object> dependencyOptions(DependencyRequest request) {
    var options = new LinkedHashMap<String, Object>();
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

  private static Map<String, Object> listOptions(ReleaseListRequest request) {
    var options = new LinkedHashMap<String, Object>();
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

  private static Map<String, Object> testOptions(TestRequest request) {
    var options = new LinkedHashMap<String, Object>();
    putIfNonNull(options, "namespace", request.namespace());
    putIfNonNull(options, "timeout", durationString(request.timeout()));
    if (!request.filter().isEmpty()) {
      options.put("filter", request.filter());
    }
    return options;
  }

  private static Map<String, Object> registryLoginOptions(RegistryLoginRequest request) {
    var options = new LinkedHashMap<String, Object>();
    putIfNonNull(options, "username", request.username());
    putIfNonNull(options, "password", request.password());
    putIfNonNull(options, "certFile", request.certificateFile());
    putIfNonNull(options, "keyFile", request.keyFile());
    putIfNonNull(options, "caFile", request.certificateAuthorityFile());
    options.put("insecure", request.insecure());
    options.put("plainHttp", request.plainHttp());
    return options;
  }

  private byte[] runGet(GetMode mode, GetRequest request) {
    var operation = "get " + mode.wireValue();
    log.debug("Get operation: mode={}, release={}", mode.wireValue(), request.releaseName());
    return invoke(
        () ->
            bridge.get(
                utf8(mode.wireValue()),
                utf8(request.releaseName()),
                toJsonBytes(getOptions(request), operation)),
        operation,
        "invokeNative");
  }

  private static ReleaseInfo mapReleasePayload(NativeReleasePayload r) {
    return new ReleaseInfo(
        r.name(),
        r.namespace(),
        r.revision(),
        r.status(),
        r.description(),
        r.firstDeployed(),
        r.lastDeployed(),
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
