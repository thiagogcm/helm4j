package dev.nthings.helm4j.internal.runtime;

import dev.nthings.helm4j.VersionInfo;
import dev.nthings.helm4j.chart.ChartRef;
import dev.nthings.helm4j.chart.DependencyRequest;
import dev.nthings.helm4j.chart.DependencyResult;
import dev.nthings.helm4j.chart.HubChartSummary;
import dev.nthings.helm4j.chart.HubSearchRequest;
import dev.nthings.helm4j.chart.LintRequest;
import dev.nthings.helm4j.chart.LintResult;
import dev.nthings.helm4j.chart.PackageChartRequest;
import dev.nthings.helm4j.chart.PackageChartResult;
import dev.nthings.helm4j.chart.PullRequest;
import dev.nthings.helm4j.chart.PullResult;
import dev.nthings.helm4j.chart.PushRequest;
import dev.nthings.helm4j.chart.PushResult;
import dev.nthings.helm4j.chart.RepoChartSummary;
import dev.nthings.helm4j.chart.RepoSearchRequest;
import dev.nthings.helm4j.chart.ShowMode;
import dev.nthings.helm4j.chart.ShowRequest;
import dev.nthings.helm4j.chart.ShowResult;
import dev.nthings.helm4j.chart.TemplateRequest;
import dev.nthings.helm4j.chart.TemplateResult;
import dev.nthings.helm4j.internal.gateway.HelmGateway;
import dev.nthings.helm4j.model.ListResult;
import dev.nthings.helm4j.release.GetAllResult;
import dev.nthings.helm4j.release.GetHooksResult;
import dev.nthings.helm4j.release.GetManifestResult;
import dev.nthings.helm4j.release.GetMetadataResult;
import dev.nthings.helm4j.release.GetNotesResult;
import dev.nthings.helm4j.release.GetRequest;
import dev.nthings.helm4j.release.GetValuesResult;
import dev.nthings.helm4j.release.HistoryEntry;
import dev.nthings.helm4j.release.HistoryRequest;
import dev.nthings.helm4j.release.InstallRequest;
import dev.nthings.helm4j.release.ReleaseInfo;
import dev.nthings.helm4j.release.ReleaseListRequest;
import dev.nthings.helm4j.release.ReleaseOutcome;
import dev.nthings.helm4j.release.RollbackRequest;
import dev.nthings.helm4j.release.StatusRequest;
import dev.nthings.helm4j.release.StatusResult;
import dev.nthings.helm4j.release.TestRequest;
import dev.nthings.helm4j.release.TestResult;
import dev.nthings.helm4j.release.UninstallRequest;
import dev.nthings.helm4j.release.UpgradeRequest;
import dev.nthings.helm4j.repo.RegistryLoginRequest;
import dev.nthings.helm4j.repo.RegistryLogoutRequest;
import dev.nthings.helm4j.repo.RegistryResult;
import dev.nthings.helm4j.repo.RepoAddRequest;
import dev.nthings.helm4j.repo.RepoAddResult;
import dev.nthings.helm4j.repo.RepoRemoveRequest;
import dev.nthings.helm4j.repo.RepoSummary;
import dev.nthings.helm4j.repo.RepoUpdateEntry;
import dev.nthings.helm4j.repo.RepoUpdateRequest;

import tools.jackson.databind.ObjectMapper;

/**
 * Aggregate {@link HelmGateway} backed by libhelm4j's JSON bridge.
 *
 * <p>This is a thin composite: it wires a shared {@link NativeGatewaySupport} into one gateway per
 * domain ({@link NativeRepoGateway}, {@link NativeChartGateway}, {@link NativeReleaseGateway},
 * {@link NativeSystemGateway}) and dispatches each operation to its owner. The translation logic
 * lives in those per-domain gateways, not here.
 */
public final class NativeStructGateway implements HelmGateway {

  private final NativeRepoGateway repo;
  private final NativeChartGateway chart;
  private final NativeReleaseGateway release;
  private final NativeSystemGateway system;

  public NativeStructGateway(HelmBridge bridge, ObjectMapper mapper) {
    var support = new NativeGatewaySupport(bridge, mapper);
    this.repo = new NativeRepoGateway(support);
    this.chart = new NativeChartGateway(support);
    this.release = new NativeReleaseGateway(support);
    this.system = new NativeSystemGateway(support);
  }

  @Override
  public RepoAddResult repoAdd(RepoAddRequest request) {
    return repo.repoAdd(request);
  }

  @Override
  public ListResult<RepoUpdateEntry> repoUpdate(RepoUpdateRequest request) {
    return repo.repoUpdate(request);
  }

  @Override
  public ListResult<RepoSummary> repoList() {
    return repo.repoList();
  }

  @Override
  public ListResult<String> repoRemove(RepoRemoveRequest request) {
    return repo.repoRemove(request);
  }

  @Override
  public RegistryResult registryLogin(RegistryLoginRequest request) {
    return repo.registryLogin(request);
  }

  @Override
  public RegistryResult registryLogout(RegistryLogoutRequest request) {
    return repo.registryLogout(request);
  }

  @Override
  public ListResult<RepoChartSummary> searchRepo(RepoSearchRequest request) {
    return chart.searchRepo(request);
  }

  @Override
  public ListResult<HubChartSummary> searchHub(HubSearchRequest request) {
    return chart.searchHub(request);
  }

  @Override
  public PullResult pull(PullRequest request) {
    return chart.pull(request);
  }

  @Override
  public PushResult push(PushRequest request) {
    return chart.push(request);
  }

  @Override
  public PackageChartResult packageChart(PackageChartRequest request) {
    return chart.packageChart(request);
  }

  @Override
  public DependencyResult dependency(DependencyRequest request) {
    return chart.dependency(request);
  }

  @Override
  public ShowResult show(ShowMode mode, ChartRef chartReference, ShowRequest request) {
    return chart.show(mode, chartReference, request);
  }

  @Override
  public TemplateResult template(TemplateRequest request) {
    return chart.template(request);
  }

  @Override
  public LintResult lint(LintRequest request) {
    return chart.lint(request);
  }

  @Override
  public ReleaseOutcome install(InstallRequest request) {
    return release.install(request);
  }

  @Override
  public ReleaseOutcome upgrade(UpgradeRequest request) {
    return release.upgrade(request);
  }

  @Override
  public ReleaseOutcome uninstall(UninstallRequest request) {
    return release.uninstall(request);
  }

  @Override
  public StatusResult status(StatusRequest request) {
    return release.status(request);
  }

  @Override
  public ReleaseOutcome rollback(RollbackRequest request) {
    return release.rollback(request);
  }

  @Override
  public ListResult<HistoryEntry> history(HistoryRequest request) {
    return release.history(request);
  }

  @Override
  public ListResult<ReleaseInfo> list(ReleaseListRequest request) {
    return release.list(request);
  }

  @Override
  public TestResult test(TestRequest request) {
    return release.test(request);
  }

  @Override
  public GetAllResult getAll(GetRequest request) {
    return release.getAll(request);
  }

  @Override
  public GetValuesResult getValues(GetRequest request) {
    return release.getValues(request);
  }

  @Override
  public GetManifestResult getManifest(GetRequest request) {
    return release.getManifest(request);
  }

  @Override
  public GetHooksResult getHooks(GetRequest request) {
    return release.getHooks(request);
  }

  @Override
  public GetNotesResult getNotes(GetRequest request) {
    return release.getNotes(request);
  }

  @Override
  public GetMetadataResult getMetadata(GetRequest request) {
    return release.getMetadata(request);
  }

  @Override
  public VersionInfo version() {
    return system.version();
  }
}
