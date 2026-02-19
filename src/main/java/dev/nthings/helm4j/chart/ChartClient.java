package dev.nthings.helm4j.chart;

import java.util.Objects;
import java.util.function.Consumer;

import dev.nthings.helm4j.internal.api.ClientSupport;
import dev.nthings.helm4j.internal.sdk.HelmGateway;

/** Chart namespace for search and chart-content operations. */
public final class ChartClient {

  private final HelmGateway gateway;

  public ChartClient(HelmGateway gateway) {
    this.gateway = Objects.requireNonNull(gateway, "gateway");
  }

  public RepoSearchResult searchRepo(Consumer<RepoSearchRequest.Builder> spec) {
    return ClientSupport.buildAndCall(
        RepoSearchRequest::builder, spec, RepoSearchRequest.Builder::build, this::searchRepo);
  }

  public RepoSearchResult searchRepo(RepoSearchRequest request) {
    Objects.requireNonNull(request, "request");
    return gateway.searchRepo(request);
  }

  public HubSearchResult searchHub(Consumer<HubSearchRequest.Builder> spec) {
    return ClientSupport.buildAndCall(
        HubSearchRequest::builder, spec, HubSearchRequest.Builder::build, this::searchHub);
  }

  public HubSearchResult searchHub(HubSearchRequest request) {
    Objects.requireNonNull(request, "request");
    return gateway.searchHub(request);
  }

  public ShowResult show(
      ShowMode mode, ChartRef chartReference, Consumer<ShowRequest.Builder> spec) {
    Objects.requireNonNull(mode, "mode");
    Objects.requireNonNull(chartReference, "chartReference");
    return ClientSupport.buildAndCall(
        ShowRequest::builder,
        spec,
        ShowRequest.Builder::build,
        request -> show(mode, chartReference, request));
  }

  public ShowResult show(ShowMode mode, ChartRef chartReference, ShowRequest request) {
    Objects.requireNonNull(mode, "mode");
    Objects.requireNonNull(chartReference, "chartReference");
    Objects.requireNonNull(request, "request");
    return gateway.show(mode, chartReference, request);
  }

  public TemplateResult template(Consumer<TemplateRequest.Builder> spec) {
    return ClientSupport.buildAndCall(
        TemplateRequest::builder, spec, TemplateRequest.Builder::build, this::template);
  }

  public TemplateResult template(TemplateRequest request) {
    Objects.requireNonNull(request, "request");
    return gateway.template(request);
  }

  public LintResult lint(Consumer<LintRequest.Builder> spec) {
    return ClientSupport.buildAndCall(
        LintRequest::builder, spec, LintRequest.Builder::build, this::lint);
  }

  public LintResult lint(LintRequest request) {
    Objects.requireNonNull(request, "request");
    return gateway.lint(request);
  }

  public PullResult pull(Consumer<PullRequest.Builder> spec) {
    return ClientSupport.buildAndCall(
        PullRequest::builder, spec, PullRequest.Builder::build, this::pull);
  }

  public PullResult pull(PullRequest request) {
    Objects.requireNonNull(request, "request");
    return gateway.pull(request);
  }

  public PushResult push(Consumer<PushRequest.Builder> spec) {
    return ClientSupport.buildAndCall(
        PushRequest::builder, spec, PushRequest.Builder::build, this::push);
  }

  public PushResult push(PushRequest request) {
    Objects.requireNonNull(request, "request");
    return gateway.push(request);
  }

  public PackageChartResult packageChart(Consumer<PackageChartRequest.Builder> spec) {
    return ClientSupport.buildAndCall(
        PackageChartRequest::builder, spec, PackageChartRequest.Builder::build, this::packageChart);
  }

  public PackageChartResult packageChart(PackageChartRequest request) {
    Objects.requireNonNull(request, "request");
    return gateway.packageChart(request);
  }

  public DependencyResult dependency(Consumer<DependencyRequest.Builder> spec) {
    return ClientSupport.buildAndCall(
        DependencyRequest::builder, spec, DependencyRequest.Builder::build, this::dependency);
  }

  public DependencyResult dependency(DependencyRequest request) {
    Objects.requireNonNull(request, "request");
    return gateway.dependency(request);
  }
}
