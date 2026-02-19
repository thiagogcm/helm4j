package dev.nthings.helm4j.internal.sdk;

import dev.nthings.helm4j.VersionInfo;
import dev.nthings.helm4j.chart.ChartRef;
import dev.nthings.helm4j.chart.DependencyRequest;
import dev.nthings.helm4j.chart.DependencyResult;
import dev.nthings.helm4j.chart.HubSearchRequest;
import dev.nthings.helm4j.chart.HubSearchResult;
import dev.nthings.helm4j.chart.LintRequest;
import dev.nthings.helm4j.chart.LintResult;
import dev.nthings.helm4j.chart.PackageChartRequest;
import dev.nthings.helm4j.chart.PackageChartResult;
import dev.nthings.helm4j.chart.PullRequest;
import dev.nthings.helm4j.chart.PullResult;
import dev.nthings.helm4j.chart.PushRequest;
import dev.nthings.helm4j.chart.PushResult;
import dev.nthings.helm4j.chart.RepoSearchRequest;
import dev.nthings.helm4j.chart.RepoSearchResult;
import dev.nthings.helm4j.chart.ShowMode;
import dev.nthings.helm4j.chart.ShowRequest;
import dev.nthings.helm4j.chart.ShowResult;
import dev.nthings.helm4j.chart.TemplateRequest;
import dev.nthings.helm4j.chart.TemplateResult;
import dev.nthings.helm4j.release.GetAllResult;
import dev.nthings.helm4j.release.GetHooksResult;
import dev.nthings.helm4j.release.GetManifestResult;
import dev.nthings.helm4j.release.GetMetadataResult;
import dev.nthings.helm4j.release.GetNotesResult;
import dev.nthings.helm4j.release.GetRequest;
import dev.nthings.helm4j.release.GetValuesResult;
import dev.nthings.helm4j.release.HistoryRequest;
import dev.nthings.helm4j.release.HistoryResult;
import dev.nthings.helm4j.release.InstallRequest;
import dev.nthings.helm4j.release.InstallResult;
import dev.nthings.helm4j.release.ReleaseListRequest;
import dev.nthings.helm4j.release.ReleaseListResult;
import dev.nthings.helm4j.release.RollbackRequest;
import dev.nthings.helm4j.release.RollbackResult;
import dev.nthings.helm4j.release.StatusRequest;
import dev.nthings.helm4j.release.StatusResult;
import dev.nthings.helm4j.release.TestRequest;
import dev.nthings.helm4j.release.TestResult;
import dev.nthings.helm4j.release.UninstallRequest;
import dev.nthings.helm4j.release.UninstallResult;
import dev.nthings.helm4j.release.UpgradeRequest;
import dev.nthings.helm4j.release.UpgradeResult;
import dev.nthings.helm4j.repo.RegistryLoginRequest;
import dev.nthings.helm4j.repo.RegistryLogoutRequest;
import dev.nthings.helm4j.repo.RegistryResult;
import dev.nthings.helm4j.repo.RepoAddRequest;
import dev.nthings.helm4j.repo.RepoAddResult;
import dev.nthings.helm4j.repo.RepoListResult;
import dev.nthings.helm4j.repo.RepoRemoveRequest;
import dev.nthings.helm4j.repo.RepoRemoveResult;
import dev.nthings.helm4j.repo.RepoUpdateRequest;
import dev.nthings.helm4j.repo.RepoUpdateResult;

/** Internal gateway used by the public SDK namespaces. */
public interface HelmGateway {

  RepoAddResult repoAdd(RepoAddRequest request);

  RepoUpdateResult repoUpdate(RepoUpdateRequest request);

  RepoListResult repoList();

  RepoRemoveResult repoRemove(RepoRemoveRequest request);

  RepoSearchResult searchRepo(RepoSearchRequest request);

  HubSearchResult searchHub(HubSearchRequest request);

  PullResult pull(PullRequest request);

  PushResult push(PushRequest request);

  PackageChartResult packageChart(PackageChartRequest request);

  DependencyResult dependency(DependencyRequest request);

  ShowResult show(ShowMode mode, ChartRef chartReference, ShowRequest request);

  InstallResult install(InstallRequest request);

  UpgradeResult upgrade(UpgradeRequest request);

  UninstallResult uninstall(UninstallRequest request);

  StatusResult status(StatusRequest request);

  RollbackResult rollback(RollbackRequest request);

  HistoryResult history(HistoryRequest request);

  ReleaseListResult list(ReleaseListRequest request);

  TestResult test(TestRequest request);

  GetAllResult getAll(GetRequest request);

  GetValuesResult getValues(GetRequest request);

  GetManifestResult getManifest(GetRequest request);

  GetHooksResult getHooks(GetRequest request);

  GetNotesResult getNotes(GetRequest request);

  GetMetadataResult getMetadata(GetRequest request);

  TemplateResult template(TemplateRequest request);

  LintResult lint(LintRequest request);

  RegistryResult registryLogin(RegistryLoginRequest request);

  RegistryResult registryLogout(RegistryLogoutRequest request);

  VersionInfo version();
}
