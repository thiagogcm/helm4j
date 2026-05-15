package dev.nthings.helm4j.spi;

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
import dev.nthings.helm4j.model.ListResult;

/** Internal chart operations exposed to the chart namespace client. */
public interface ChartGateway {

  ListResult<RepoChartSummary> searchRepo(RepoSearchRequest request);

  ListResult<HubChartSummary> searchHub(HubSearchRequest request);

  PullResult pull(PullRequest request);

  PushResult push(PushRequest request);

  PackageChartResult packageChart(PackageChartRequest request);

  DependencyResult dependency(DependencyRequest request);

  ShowResult show(ShowMode mode, ChartRef chartReference, ShowRequest request);

  TemplateResult template(TemplateRequest request);

  LintResult lint(LintRequest request);
}
