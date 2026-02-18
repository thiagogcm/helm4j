package dev.nthings.helm4j.chart;

import java.util.Objects;
import java.util.function.Consumer;

import dev.nthings.helm4j.internal.sdk.HelmGateway;
import dev.nthings.helm4j.types.ChartRef;

/** Chart namespace for search and chart-content operations. */
public final class ChartClient {

  private final HelmGateway gateway;

  public ChartClient(HelmGateway gateway) {
    this.gateway = Objects.requireNonNull(gateway, "gateway");
  }

  public RepoSearchResult searchRepo(String keyword) {
    return searchRepo(RepoSearchRequest.defaults(keyword));
  }

  public RepoSearchResult searchRepo(String keyword, Consumer<RepoSearchRequest.Builder> spec) {
    Objects.requireNonNull(spec, "spec");
    var builder = RepoSearchRequest.builder().keyword(keyword);
    spec.accept(builder);
    return searchRepo(builder.build());
  }

  public RepoSearchResult searchRepo(RepoSearchRequest request) {
    Objects.requireNonNull(request, "request");
    return gateway.searchRepo(request);
  }

  public HubSearchResult searchHub(String keyword) {
    return searchHub(HubSearchRequest.defaults(keyword));
  }

  public HubSearchResult searchHub(String keyword, Consumer<HubSearchRequest.Builder> spec) {
    Objects.requireNonNull(spec, "spec");
    var builder = HubSearchRequest.builder().keyword(keyword);
    spec.accept(builder);
    return searchHub(builder.build());
  }

  public HubSearchResult searchHub(HubSearchRequest request) {
    Objects.requireNonNull(request, "request");
    return gateway.searchHub(request);
  }

  public ShowChartResult chart(ChartRef chartReference) {
    return chart(chartReference, ShowRequest.defaults());
  }

  public ShowChartResult chart(ChartRef chartReference, Consumer<ShowRequest.Builder> spec) {
    Objects.requireNonNull(spec, "spec");
    var builder = ShowRequest.builder();
    spec.accept(builder);
    return chart(chartReference, builder.build());
  }

  public ShowChartResult chart(ChartRef chartReference, ShowRequest request) {
    Objects.requireNonNull(chartReference, "chartReference");
    Objects.requireNonNull(request, "request");
    return gateway.showChart(chartReference, request);
  }

  public ShowValuesResult values(ChartRef chartReference) {
    return values(chartReference, ShowRequest.defaults());
  }

  public ShowValuesResult values(ChartRef chartReference, Consumer<ShowRequest.Builder> spec) {
    Objects.requireNonNull(spec, "spec");
    var builder = ShowRequest.builder();
    spec.accept(builder);
    return values(chartReference, builder.build());
  }

  public ShowValuesResult values(ChartRef chartReference, ShowRequest request) {
    Objects.requireNonNull(chartReference, "chartReference");
    Objects.requireNonNull(request, "request");
    return gateway.showValues(chartReference, request);
  }

  public ShowReadmeResult readme(ChartRef chartReference) {
    return readme(chartReference, ShowRequest.defaults());
  }

  public ShowReadmeResult readme(ChartRef chartReference, Consumer<ShowRequest.Builder> spec) {
    Objects.requireNonNull(spec, "spec");
    var builder = ShowRequest.builder();
    spec.accept(builder);
    return readme(chartReference, builder.build());
  }

  public ShowReadmeResult readme(ChartRef chartReference, ShowRequest request) {
    Objects.requireNonNull(chartReference, "chartReference");
    Objects.requireNonNull(request, "request");
    return gateway.showReadme(chartReference, request);
  }

  public ShowCrdsResult crds(ChartRef chartReference) {
    return crds(chartReference, ShowRequest.defaults());
  }

  public ShowCrdsResult crds(ChartRef chartReference, Consumer<ShowRequest.Builder> spec) {
    Objects.requireNonNull(spec, "spec");
    var builder = ShowRequest.builder();
    spec.accept(builder);
    return crds(chartReference, builder.build());
  }

  public ShowCrdsResult crds(ChartRef chartReference, ShowRequest request) {
    Objects.requireNonNull(chartReference, "chartReference");
    Objects.requireNonNull(request, "request");
    return gateway.showCrds(chartReference, request);
  }

  public ShowAllResult all(ChartRef chartReference) {
    return all(chartReference, ShowRequest.defaults());
  }

  public ShowAllResult all(ChartRef chartReference, Consumer<ShowRequest.Builder> spec) {
    Objects.requireNonNull(spec, "spec");
    var builder = ShowRequest.builder();
    spec.accept(builder);
    return all(chartReference, builder.build());
  }

  public ShowAllResult all(ChartRef chartReference, ShowRequest request) {
    Objects.requireNonNull(chartReference, "chartReference");
    Objects.requireNonNull(request, "request");
    return gateway.showAll(chartReference, request);
  }

  public TemplateResult template(Consumer<TemplateRequest.Builder> spec) {
    Objects.requireNonNull(spec, "spec");
    var builder = TemplateRequest.builder();
    spec.accept(builder);
    return template(builder.build());
  }

  public TemplateResult template(TemplateRequest request) {
    Objects.requireNonNull(request, "request");
    return gateway.template(request);
  }

  public LintResult lint(java.nio.file.Path chartPath) {
    return lint(LintRequest.builder().chartPath(chartPath).build());
  }

  public LintResult lint(Consumer<LintRequest.Builder> spec) {
    Objects.requireNonNull(spec, "spec");
    var builder = LintRequest.builder();
    spec.accept(builder);
    return lint(builder.build());
  }

  public LintResult lint(LintRequest request) {
    Objects.requireNonNull(request, "request");
    return gateway.lint(request);
  }
}
