package dev.nthings.helm4j.chart;

import java.util.Objects;
import java.util.function.Consumer;

import dev.nthings.helm4j.internal.api.NamespaceClient;
import dev.nthings.helm4j.internal.sdk.ChartGateway;
import dev.nthings.helm4j.model.ListResult;

/** Chart namespace for search and chart-content operations. */
public final class ChartClient extends NamespaceClient<ChartGateway> {

  public ChartClient(ChartGateway gateway) {
    super(gateway);
  }

  public ListResult<RepoChartSummary> searchRepo(Consumer<RepoSearchRequest.Builder> spec) {
    return buildAndInvoke(
        RepoSearchRequest::builder, spec, RepoSearchRequest.Builder::build, this::searchRepo);
  }

  public ListResult<RepoChartSummary> searchRepo(RepoSearchRequest request) {
    return invoke(request, gateway::searchRepo);
  }

  public ListResult<HubChartSummary> searchHub(Consumer<HubSearchRequest.Builder> spec) {
    return buildAndInvoke(
        HubSearchRequest::builder, spec, HubSearchRequest.Builder::build, this::searchHub);
  }

  public ListResult<HubChartSummary> searchHub(HubSearchRequest request) {
    return invoke(request, gateway::searchHub);
  }

  public ShowResult show(
      ShowMode mode, ChartRef chartReference, Consumer<ShowRequest.Builder> spec) {
    Objects.requireNonNull(mode, "mode");
    Objects.requireNonNull(chartReference, "chartReference");
    return buildAndInvoke(
        ShowRequest::builder,
        spec,
        ShowRequest.Builder::build,
        request -> show(mode, chartReference, request));
  }

  public ShowResult show(ShowMode mode, ChartRef chartReference, ShowRequest request) {
    Objects.requireNonNull(mode, "mode");
    Objects.requireNonNull(chartReference, "chartReference");
    return invoke(request, builtRequest -> gateway.show(mode, chartReference, builtRequest));
  }

  public TemplateResult template(Consumer<TemplateRequest.Builder> spec) {
    return buildAndInvoke(
        TemplateRequest::builder, spec, TemplateRequest.Builder::build, this::template);
  }

  public TemplateResult template(TemplateRequest request) {
    return invoke(request, gateway::template);
  }

  public LintResult lint(Consumer<LintRequest.Builder> spec) {
    return buildAndInvoke(LintRequest::builder, spec, LintRequest.Builder::build, this::lint);
  }

  public LintResult lint(LintRequest request) {
    return invoke(request, gateway::lint);
  }

  public PullResult pull(Consumer<PullRequest.Builder> spec) {
    return buildAndInvoke(PullRequest::builder, spec, PullRequest.Builder::build, this::pull);
  }

  public PullResult pull(PullRequest request) {
    return invoke(request, gateway::pull);
  }

  public PushResult push(Consumer<PushRequest.Builder> spec) {
    return buildAndInvoke(PushRequest::builder, spec, PushRequest.Builder::build, this::push);
  }

  public PushResult push(PushRequest request) {
    return invoke(request, gateway::push);
  }

  public PackageChartResult packageChart(Consumer<PackageChartRequest.Builder> spec) {
    return buildAndInvoke(
        PackageChartRequest::builder, spec, PackageChartRequest.Builder::build, this::packageChart);
  }

  public PackageChartResult packageChart(PackageChartRequest request) {
    return invoke(request, gateway::packageChart);
  }

  public DependencyResult dependency(Consumer<DependencyRequest.Builder> spec) {
    return buildAndInvoke(
        DependencyRequest::builder, spec, DependencyRequest.Builder::build, this::dependency);
  }

  public DependencyResult dependency(DependencyRequest request) {
    return invoke(request, gateway::dependency);
  }
}
