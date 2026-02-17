package dev.nthings.helm4j.internal.sdk;

import dev.nthings.helm4j.chart.HubSearchRequest;
import dev.nthings.helm4j.chart.HubSearchResult;
import dev.nthings.helm4j.chart.RepoSearchRequest;
import dev.nthings.helm4j.chart.RepoSearchResult;
import dev.nthings.helm4j.chart.ShowAllResult;
import dev.nthings.helm4j.chart.ShowChartResult;
import dev.nthings.helm4j.chart.ShowCrdsResult;
import dev.nthings.helm4j.chart.ShowReadmeResult;
import dev.nthings.helm4j.chart.ShowRequest;
import dev.nthings.helm4j.chart.ShowValuesResult;
import dev.nthings.helm4j.release.InstallRequest;
import dev.nthings.helm4j.release.InstallResult;
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
}
