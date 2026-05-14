package dev.nthings.helm4j;

import java.util.List;

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
import dev.nthings.helm4j.internal.spi.HelmGateway;
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

/**
 * In-module {@link HelmGateway} double used to drive the public namespace clients without the
 * native runtime. Records the last request it received and returns empty/typed-null responses.
 */
final class FakeHelmGateway implements HelmGateway {

  Object lastRequest;
  String lastShowMode;
  int invocations;

  private <T> T record(Object request) {
    this.lastRequest = request;
    this.invocations++;
    return null;
  }

  @Override
  public RepoAddResult repoAdd(RepoAddRequest request) {
    return record(request);
  }

  @Override
  public ListResult<RepoUpdateEntry> repoUpdate(RepoUpdateRequest request) {
    record(request);
    return ListResult.of(List.of());
  }

  @Override
  public ListResult<RepoSummary> repoList() {
    record("repoList");
    return ListResult.of(List.of());
  }

  @Override
  public ListResult<String> repoRemove(RepoRemoveRequest request) {
    record(request);
    return ListResult.of(List.of());
  }

  @Override
  public RegistryResult registryLogin(RegistryLoginRequest request) {
    return record(request);
  }

  @Override
  public RegistryResult registryLogout(RegistryLogoutRequest request) {
    return record(request);
  }

  @Override
  public ListResult<RepoChartSummary> searchRepo(RepoSearchRequest request) {
    record(request);
    return ListResult.of(List.of());
  }

  @Override
  public ListResult<HubChartSummary> searchHub(HubSearchRequest request) {
    record(request);
    return ListResult.of(List.of());
  }

  @Override
  public PullResult pull(PullRequest request) {
    return record(request);
  }

  @Override
  public PushResult push(PushRequest request) {
    return record(request);
  }

  @Override
  public PackageChartResult packageChart(PackageChartRequest request) {
    return record(request);
  }

  @Override
  public DependencyResult dependency(DependencyRequest request) {
    return record(request);
  }

  @Override
  public ShowResult show(ShowMode mode, ChartRef chartReference, ShowRequest request) {
    this.lastShowMode = mode.wireValue();
    return record(request);
  }

  @Override
  public TemplateResult template(TemplateRequest request) {
    return record(request);
  }

  @Override
  public LintResult lint(LintRequest request) {
    return record(request);
  }

  @Override
  public ReleaseOutcome install(InstallRequest request) {
    return record(request);
  }

  @Override
  public ReleaseOutcome upgrade(UpgradeRequest request) {
    return record(request);
  }

  @Override
  public ReleaseOutcome uninstall(UninstallRequest request) {
    return record(request);
  }

  @Override
  public StatusResult status(StatusRequest request) {
    return record(request);
  }

  @Override
  public ReleaseOutcome rollback(RollbackRequest request) {
    return record(request);
  }

  @Override
  public ListResult<HistoryEntry> history(HistoryRequest request) {
    record(request);
    return ListResult.of(List.of());
  }

  @Override
  public ListResult<ReleaseInfo> list(ReleaseListRequest request) {
    record(request);
    return ListResult.of(List.of());
  }

  @Override
  public TestResult test(TestRequest request) {
    return record(request);
  }

  @Override
  public GetAllResult getAll(GetRequest request) {
    return record(request);
  }

  @Override
  public GetValuesResult getValues(GetRequest request) {
    return record(request);
  }

  @Override
  public GetManifestResult getManifest(GetRequest request) {
    return record(request);
  }

  @Override
  public GetHooksResult getHooks(GetRequest request) {
    return record(request);
  }

  @Override
  public GetNotesResult getNotes(GetRequest request) {
    return record(request);
  }

  @Override
  public GetMetadataResult getMetadata(GetRequest request) {
    return record(request);
  }

  @Override
  public VersionInfo version() {
    record("version");
    return new VersionInfo("0.1.0", "go1.26", "v4.1.1");
  }
}
