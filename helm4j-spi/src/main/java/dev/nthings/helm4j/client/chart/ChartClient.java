package dev.nthings.helm4j.client.chart;

import java.util.Objects;
import java.util.function.Consumer;

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
import dev.nthings.helm4j.client.internal.NamespaceClient;
import dev.nthings.helm4j.model.ListResult;
import dev.nthings.helm4j.spi.ChartGateway;

/**
 * Chart namespace for search and chart-content operations.
 *
 * <p>Each operation has two entry points: one that takes a {@link Consumer} configuring a fluent
 * request builder, and an overload that takes a pre-built request for reuse.
 */
public final class ChartClient extends NamespaceClient<ChartGateway> {

  public ChartClient(ChartGateway gateway) {
    super(gateway);
  }

  public ListResult<RepoChartSummary> searchRepo(Consumer<RepoSearchRequest.Builder> spec) {
    var builder = RepoSearchRequest.builder();
    spec.accept(builder);
    return gateway.searchRepo(builder.build());
  }

  public ListResult<RepoChartSummary> searchRepo(RepoSearchRequest request) {
    return gateway.searchRepo(request);
  }

  public ListResult<HubChartSummary> searchHub(Consumer<HubSearchRequest.Builder> spec) {
    var builder = HubSearchRequest.builder();
    spec.accept(builder);
    return gateway.searchHub(builder.build());
  }

  public ListResult<HubChartSummary> searchHub(HubSearchRequest request) {
    return gateway.searchHub(request);
  }

  public ShowResult show(ShowMode mode, ChartRef chart, Consumer<ShowRequest.Builder> spec) {
    Objects.requireNonNull(mode, "mode");
    Objects.requireNonNull(chart, "chart");
    var builder = ShowRequest.builder();
    spec.accept(builder);
    return gateway.show(mode, chart, builder.build());
  }

  public ShowResult show(ShowMode mode, ChartRef chart, ShowRequest request) {
    Objects.requireNonNull(mode, "mode");
    Objects.requireNonNull(chart, "chart");
    return gateway.show(mode, chart, request);
  }

  public TemplateResult template(Consumer<TemplateRequest.Builder> spec) {
    var builder = TemplateRequest.builder();
    spec.accept(builder);
    return gateway.template(builder.build());
  }

  public TemplateResult template(TemplateRequest request) {
    return gateway.template(request);
  }

  public LintResult lint(Consumer<LintRequest.Builder> spec) {
    var builder = LintRequest.builder();
    spec.accept(builder);
    return gateway.lint(builder.build());
  }

  public LintResult lint(LintRequest request) {
    return gateway.lint(request);
  }

  public PullResult pull(Consumer<PullRequest.Builder> spec) {
    var builder = PullRequest.builder();
    spec.accept(builder);
    return gateway.pull(builder.build());
  }

  public PullResult pull(PullRequest request) {
    return gateway.pull(request);
  }

  public PushResult push(Consumer<PushRequest.Builder> spec) {
    var builder = PushRequest.builder();
    spec.accept(builder);
    return gateway.push(builder.build());
  }

  public PushResult push(PushRequest request) {
    return gateway.push(request);
  }

  public PackageChartResult packageChart(Consumer<PackageChartRequest.Builder> spec) {
    var builder = PackageChartRequest.builder();
    spec.accept(builder);
    return gateway.packageChart(builder.build());
  }

  public PackageChartResult packageChart(PackageChartRequest request) {
    return gateway.packageChart(request);
  }

  public DependencyResult dependency(Consumer<DependencyRequest.Builder> spec) {
    var builder = DependencyRequest.builder();
    spec.accept(builder);
    return gateway.dependency(builder.build());
  }

  public DependencyResult dependency(DependencyRequest request) {
    return gateway.dependency(request);
  }
}
