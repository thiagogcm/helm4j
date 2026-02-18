package dev.nthings.helm4j.internal.sdk;

import dev.nthings.helm4j.VersionInfo;
import dev.nthings.helm4j.chart.HubSearchRequest;
import dev.nthings.helm4j.chart.HubSearchResult;
import dev.nthings.helm4j.chart.LintRequest;
import dev.nthings.helm4j.chart.LintResult;
import dev.nthings.helm4j.chart.RepoSearchRequest;
import dev.nthings.helm4j.chart.RepoSearchResult;
import dev.nthings.helm4j.chart.ShowAllResult;
import dev.nthings.helm4j.chart.ShowChartResult;
import dev.nthings.helm4j.chart.ShowCrdsResult;
import dev.nthings.helm4j.chart.ShowReadmeResult;
import dev.nthings.helm4j.chart.ShowRequest;
import dev.nthings.helm4j.chart.ShowValuesResult;
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
import dev.nthings.helm4j.release.RollbackRequest;
import dev.nthings.helm4j.release.RollbackResult;
import dev.nthings.helm4j.release.StatusRequest;
import dev.nthings.helm4j.release.StatusResult;
import dev.nthings.helm4j.release.UninstallRequest;
import dev.nthings.helm4j.release.UninstallResult;
import dev.nthings.helm4j.release.UpgradeRequest;
import dev.nthings.helm4j.release.UpgradeResult;
import dev.nthings.helm4j.repo.RepoAddRequest;
import dev.nthings.helm4j.repo.RepoAddResult;
import dev.nthings.helm4j.repo.RepoListResult;
import dev.nthings.helm4j.repo.RepoRemoveRequest;
import dev.nthings.helm4j.repo.RepoRemoveResult;
import dev.nthings.helm4j.repo.RepoUpdateRequest;
import dev.nthings.helm4j.repo.RepoUpdateResult;
import dev.nthings.helm4j.types.ChartRef;

/** Internal gateway used by the public SDK namespaces. */
public interface HelmGateway {

  RepoAddResult repoAdd(RepoAddRequest request);

  RepoUpdateResult repoUpdate(RepoUpdateRequest request);

  RepoListResult repoList();

  RepoRemoveResult repoRemove(RepoRemoveRequest request);

  RepoSearchResult searchRepo(RepoSearchRequest request);

  HubSearchResult searchHub(HubSearchRequest request);

  ShowChartResult showChart(ChartRef chartReference, ShowRequest request);

  ShowValuesResult showValues(ChartRef chartReference, ShowRequest request);

  ShowReadmeResult showReadme(ChartRef chartReference, ShowRequest request);

  ShowCrdsResult showCrds(ChartRef chartReference, ShowRequest request);

  ShowAllResult showAll(ChartRef chartReference, ShowRequest request);

  InstallResult install(InstallRequest request);

  UpgradeResult upgrade(UpgradeRequest request);

  UninstallResult uninstall(UninstallRequest request);

  StatusResult status(StatusRequest request);

  RollbackResult rollback(RollbackRequest request);

  HistoryResult history(HistoryRequest request);

  GetAllResult getAll(GetRequest request);

  GetValuesResult getValues(GetRequest request);

  GetManifestResult getManifest(GetRequest request);

  GetHooksResult getHooks(GetRequest request);

  GetNotesResult getNotes(GetRequest request);

  GetMetadataResult getMetadata(GetRequest request);

  TemplateResult template(TemplateRequest request);

  LintResult lint(LintRequest request);

  VersionInfo version();
}
