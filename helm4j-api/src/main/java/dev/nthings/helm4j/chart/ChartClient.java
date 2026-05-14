package dev.nthings.helm4j.chart;

import java.util.Objects;

import dev.nthings.helm4j.internal.api.NamespaceClient;
import dev.nthings.helm4j.internal.gateway.ChartGateway;
import dev.nthings.helm4j.model.ListResult;

/**
 * Chart namespace for search and chart-content operations.
 *
 * <p>Each operation has two entry points: a method that returns a runnable, fluent request builder
 * (call {@code execute()} on it), and an overload that takes a pre-built request for reuse.
 */
public final class ChartClient extends NamespaceClient<ChartGateway> {

  public ChartClient(ChartGateway gateway) {
    super(gateway);
  }

  /** Begins a fluent repository search; call {@code execute()} to run it. */
  public RepoSearchRequest.Builder searchRepo() {
    return RepoSearchRequest.builder(gateway);
  }

  public ListResult<RepoChartSummary> searchRepo(RepoSearchRequest request) {
    return gateway.searchRepo(request);
  }

  /** Begins a fluent hub search; call {@code execute()} to run it. */
  public HubSearchRequest.Builder searchHub() {
    return HubSearchRequest.builder(gateway);
  }

  public ListResult<HubChartSummary> searchHub(HubSearchRequest request) {
    return gateway.searchHub(request);
  }

  /** Begins a fluent show operation for the given chart; call {@code execute()} to run it. */
  public ShowRequest.Builder show(ShowMode mode, ChartRef chart) {
    Objects.requireNonNull(mode, "mode");
    Objects.requireNonNull(chart, "chart");
    return ShowRequest.builder(gateway, mode, chart);
  }

  public ShowResult show(ShowMode mode, ChartRef chart, ShowRequest request) {
    Objects.requireNonNull(mode, "mode");
    Objects.requireNonNull(chart, "chart");
    return gateway.show(mode, chart, request);
  }

  /** Begins a fluent template render; call {@code execute()} to run it. */
  public TemplateRequest.Builder template() {
    return TemplateRequest.builder(gateway);
  }

  public TemplateResult template(TemplateRequest request) {
    return gateway.template(request);
  }

  /** Begins a fluent lint; call {@code execute()} to run it. */
  public LintRequest.Builder lint() {
    return LintRequest.builder(gateway);
  }

  public LintResult lint(LintRequest request) {
    return gateway.lint(request);
  }

  /** Begins a fluent pull; call {@code execute()} to run it. */
  public PullRequest.Builder pull() {
    return PullRequest.builder(gateway);
  }

  public PullResult pull(PullRequest request) {
    return gateway.pull(request);
  }

  /** Begins a fluent push; call {@code execute()} to run it. */
  public PushRequest.Builder push() {
    return PushRequest.builder(gateway);
  }

  public PushResult push(PushRequest request) {
    return gateway.push(request);
  }

  /** Begins a fluent chart packaging; call {@code execute()} to run it. */
  public PackageChartRequest.Builder packageChart() {
    return PackageChartRequest.builder(gateway);
  }

  public PackageChartResult packageChart(PackageChartRequest request) {
    return gateway.packageChart(request);
  }

  /** Begins a fluent dependency resolution; call {@code execute()} to run it. */
  public DependencyRequest.Builder dependency() {
    return DependencyRequest.builder(gateway);
  }

  public DependencyResult dependency(DependencyRequest request) {
    return gateway.dependency(request);
  }
}
