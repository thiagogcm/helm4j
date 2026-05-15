package dev.nthings.helm4j.client.charts;

import java.util.Objects;
import java.util.function.Consumer;

import dev.nthings.helm4j.chart.ChartRef;
import dev.nthings.helm4j.chart.DependencyRequest;
import dev.nthings.helm4j.chart.DependencyResult;
import dev.nthings.helm4j.chart.HubChartSummary;
import dev.nthings.helm4j.chart.LintRequest;
import dev.nthings.helm4j.chart.LintResult;
import dev.nthings.helm4j.chart.PackageChartRequest;
import dev.nthings.helm4j.chart.PackageChartResult;
import dev.nthings.helm4j.chart.PullRequest;
import dev.nthings.helm4j.chart.PullResult;
import dev.nthings.helm4j.chart.PushRequest;
import dev.nthings.helm4j.chart.PushResult;
import dev.nthings.helm4j.chart.RepoChartSummary;
import dev.nthings.helm4j.chart.SearchCharts;
import dev.nthings.helm4j.chart.SearchHub;
import dev.nthings.helm4j.chart.ShowMode;
import dev.nthings.helm4j.chart.ShowRequest;
import dev.nthings.helm4j.chart.ShowResult;
import dev.nthings.helm4j.chart.TemplateRequest;
import dev.nthings.helm4j.chart.TemplateResult;
import dev.nthings.helm4j.client.internal.NamespaceClient;
import dev.nthings.helm4j.model.ListResult;
import dev.nthings.helm4j.spi.ChartGateway;

/** Chart namespace: search, show, render, package, transfer, lint. */
public final class ChartsClient extends NamespaceClient<ChartGateway> {

  public ChartsClient(ChartGateway gateway) {
    super(gateway);
  }

  public ListResult<RepoChartSummary> searchRepository(Consumer<SearchCharts.Builder> spec) {
    return gateway.searchRepository(configured(SearchCharts::builder, spec).build());
  }

  public ListResult<RepoChartSummary> searchRepository(SearchCharts request) {
    return gateway.searchRepository(request);
  }

  public ListResult<HubChartSummary> searchHub(Consumer<SearchHub.Builder> spec) {
    return gateway.searchHub(configured(SearchHub::builder, spec).build());
  }

  public ListResult<HubChartSummary> searchHub(SearchHub request) {
    return gateway.searchHub(request);
  }

  public ShowResult show(ShowMode mode, ChartRef chart, Consumer<ShowRequest.Builder> spec) {
    Objects.requireNonNull(mode, "mode");
    Objects.requireNonNull(chart, "chart");
    return gateway.show(mode, chart, configured(ShowRequest::builder, spec).build());
  }

  public ShowResult show(ShowMode mode, ChartRef chart, ShowRequest request) {
    Objects.requireNonNull(mode, "mode");
    Objects.requireNonNull(chart, "chart");
    return gateway.show(mode, chart, request);
  }

  public TemplateResult template(Consumer<TemplateRequest.Builder> spec) {
    return gateway.template(configured(TemplateRequest::builder, spec).build());
  }

  public TemplateResult template(TemplateRequest request) {
    return gateway.template(request);
  }

  public LintResult lint(Consumer<LintRequest.Builder> spec) {
    return gateway.lint(configured(LintRequest::builder, spec).build());
  }

  public LintResult lint(LintRequest request) {
    return gateway.lint(request);
  }

  public PullResult pull(Consumer<PullRequest.Builder> spec) {
    return gateway.pull(configured(PullRequest::builder, spec).build());
  }

  public PullResult pull(PullRequest request) {
    return gateway.pull(request);
  }

  public PushResult push(Consumer<PushRequest.Builder> spec) {
    return gateway.push(configured(PushRequest::builder, spec).build());
  }

  public PushResult push(PushRequest request) {
    return gateway.push(request);
  }

  public PackageChartResult packageChart(Consumer<PackageChartRequest.Builder> spec) {
    return gateway.packageChart(configured(PackageChartRequest::builder, spec).build());
  }

  public PackageChartResult packageChart(PackageChartRequest request) {
    return gateway.packageChart(request);
  }

  public DependencyResult dependency(Consumer<DependencyRequest.Builder> spec) {
    return gateway.dependency(configured(DependencyRequest::builder, spec).build());
  }

  public DependencyResult dependency(DependencyRequest request) {
    return gateway.dependency(request);
  }
}
