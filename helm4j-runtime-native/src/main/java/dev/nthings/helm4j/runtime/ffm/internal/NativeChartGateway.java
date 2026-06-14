package dev.nthings.helm4j.runtime.ffm.internal;

import java.util.List;
import java.util.Objects;

import dev.nthings.helm4j.chart.ChartRef;
import dev.nthings.helm4j.chart.DependencyRequest;
import dev.nthings.helm4j.chart.DependencyResult;
import dev.nthings.helm4j.chart.HubChartSummary;
import dev.nthings.helm4j.chart.LintMessage;
import dev.nthings.helm4j.chart.LintRequest;
import dev.nthings.helm4j.chart.LintResult;
import dev.nthings.helm4j.chart.LintSeverity;
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
import dev.nthings.helm4j.errors.HelmConfigurationException;
import dev.nthings.helm4j.model.ListResult;
import dev.nthings.helm4j.spi.ChartGateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dev.nthings.helm4j.runtime.ffm.internal.NativeGatewaySupport.listOrEmpty;
import static dev.nthings.helm4j.runtime.ffm.internal.NativeGatewaySupport.mapReleasePayload;
import static dev.nthings.helm4j.runtime.ffm.internal.NativeGatewaySupport.requireResponse;
import static dev.nthings.helm4j.runtime.ffm.internal.NativeGatewaySupport.runtimeFailure;
import static dev.nthings.helm4j.runtime.ffm.internal.NativeGatewaySupport.utf8;

/** Chart-oriented operations (search, pull/push, package, dependency, show, template, lint). */
final class NativeChartGateway implements ChartGateway {

  private static final Logger log = LoggerFactory.getLogger(NativeChartGateway.class);

  private final NativeGatewaySupport support;

  NativeChartGateway(NativeGatewaySupport support) {
    this.support = Objects.requireNonNull(support, "support");
  }

  @Override
  public ListResult<RepoChartSummary> searchRepository(SearchCharts request) {
    Objects.requireNonNull(request, "request");

    log.debug("Searching repositories: keyword={}", request.keyword());
    var root =
        support.invokeRootOrThrow(
            "search repo",
            bridge ->
                bridge.search(
                    utf8("repo"),
                    support.toJsonBytes(NativeOptions.searchRepo(request), "search repo")));

    var response = support.convert(root, SearchPayload.class, "search repo");
    var charts =
        listOrEmpty(response == null ? null : response.results()).stream()
            .map(
                entry ->
                    new RepoChartSummary(
                        entry.name(),
                        entry.version(),
                        entry.appVersion(),
                        entry.description(),
                        entry.score(),
                        entry.repositoryName(),
                        entry.repositoryUrl()))
            .toList();
    return ListResult.of(charts);
  }

  @Override
  public ListResult<HubChartSummary> searchHub(SearchHub request) {
    Objects.requireNonNull(request, "request");

    log.debug("Searching hub: keyword={}", request.keyword());
    var root =
        support.invokeRootOrThrow(
            "search hub",
            bridge ->
                bridge.search(
                    utf8("hub"),
                    support.toJsonBytes(NativeOptions.searchHub(request), "search hub")));

    var response = support.convert(root, SearchPayload.class, "search hub");
    var charts =
        listOrEmpty(response == null ? null : response.results()).stream()
            .map(
                entry ->
                    new HubChartSummary(
                        entry.name(),
                        entry.version(),
                        entry.appVersion(),
                        entry.description(),
                        entry.score(),
                        entry.url(),
                        entry.repositoryName(),
                        entry.repositoryUrl()))
            .toList();
    return ListResult.of(charts);
  }

  @Override
  public PullResult pull(PullRequest request) {
    Objects.requireNonNull(request, "request");

    log.debug("Pulling chart: chartRef={}", request.chart().asReference());
    var root =
        support.invokeRootOrThrow(
            "pull",
            bridge ->
                bridge.pull(
                    utf8(request.chart().asReference()),
                    support.toJsonBytes(NativeOptions.pull(request), "pull")));

    var response = support.convert(root, PullPayload.class, "pull");
    return new PullResult(response == null ? "" : response.output());
  }

  @Override
  public PushResult push(PushRequest request) {
    Objects.requireNonNull(request, "request");
    if (request.chartReference() == null || request.remote() == null) {
      throw new HelmConfigurationException("Push requires chart reference and remote");
    }

    log.debug("Pushing chart: chartRef={}, remote={}", request.chartReference(), request.remote());
    var root =
        support.invokeRootOrThrow(
            "push",
            bridge ->
                bridge.push(
                    utf8(request.chartReference()),
                    utf8(request.remote()),
                    support.toJsonBytes(NativeOptions.push(request), "push")));

    var response = support.convert(root, PushPayload.class, "push");
    return new PushResult(response == null ? "" : response.output());
  }

  @Override
  public PackageChartResult packageChart(PackageChartRequest request) {
    Objects.requireNonNull(request, "request");
    if (request.chartPath() == null) {
      throw new HelmConfigurationException("Package requires chart path");
    }

    log.debug("Packaging chart: chartPath={}", request.chartPath());
    var root =
        support.invokeRootOrThrow(
            "package",
            bridge ->
                bridge.packageChart(
                    utf8(request.chartPath().toString()),
                    support.toJsonBytes(NativeOptions.packageChart(request), "package")));

    var response = support.convert(root, PackagePayload.class, "package");
    return new PackageChartResult(response == null ? null : response.path());
  }

  @Override
  public DependencyResult dependency(DependencyRequest request) {
    Objects.requireNonNull(request, "request");
    if (request.chartPath() == null) {
      throw new HelmConfigurationException("Dependency operation requires chart path");
    }

    log.debug("Listing chart dependencies: chartPath={}", request.chartPath());
    var root =
        support.invokeRootOrThrow(
            "dependency",
            bridge ->
                bridge.dependency(
                    utf8(request.chartPath().toString()),
                    support.toJsonBytes(NativeOptions.dependency(request), "dependency")));

    var response = support.convert(root, DependencyPayload.class, "dependency");
    return new DependencyResult(response == null ? "" : response.output());
  }

  @Override
  public ShowResult show(ShowMode mode, ChartRef chartReference, ShowRequest request) {
    var response = runShow(mode, chartReference, request);
    var sections = response.sections();
    return new ShowResult(
        mode,
        response.chartRef(),
        response.chartPath(),
        sections.chart(),
        sections.values(),
        sections.readme(),
        listOrEmpty(sections.crds()),
        response.cliOutput());
  }

  @Override
  public TemplateResult template(TemplateRequest request) {
    Objects.requireNonNull(request, "request");
    log.debug(
        "Templating chart: name={}, chart={}",
        request.releaseName(),
        request.chart().asReference());
    var root =
        support.invokeRootOrThrow(
            "template",
            bridge ->
                bridge.template(
                    utf8(request.releaseName()),
                    utf8(request.chart().asReference()),
                    support.toJsonBytes(NativeOptions.template(request), "template")));

    var response =
        requireResponse(
            support.convert(root, TemplatePayload.class, "template"), "template", "data");
    return new TemplateResult(
        mapReleasePayload(requireResponse(response.release(), "template", "release"), "template"),
        response.manifest());
  }

  @Override
  public LintResult lint(LintRequest request) {
    Objects.requireNonNull(request, "request");

    log.debug("Linting chart: path={}", request.chartPath());
    var root =
        support.invokeRootOrThrow(
            "lint",
            bridge ->
                bridge.lint(
                    utf8(request.chartPath().toString()),
                    support.toJsonBytes(NativeOptions.lint(request), "lint")));

    var response = support.convert(root, LintPayload.class, "lint");
    var messages =
        listOrEmpty(response == null ? null : response.messages()).stream()
            .map(m -> new LintMessage(LintSeverity.fromWireValue(m.severity()), m.message()))
            .toList();
    return new LintResult(
        messages,
        response == null ? 0 : response.totalCharts(),
        response == null ? 0 : response.chartsTested(),
        response == null ? 0 : response.chartsFailed());
  }

  private ShowPayload runShow(ShowMode mode, ChartRef chartReference, ShowRequest request) {
    Objects.requireNonNull(mode, "mode");
    Objects.requireNonNull(chartReference, "chartReference");
    Objects.requireNonNull(request, "request");

    var operation = "show " + mode.wireValue();
    log.debug("Show operation: mode={}, chart={}", mode.wireValue(), chartReference.asReference());
    var root =
        support.invokeRootOrThrow(
            operation,
            bridge ->
                bridge.show(
                    utf8(mode.wireValue()),
                    utf8(chartReference.asReference()),
                    support.toJsonBytes(NativeOptions.show(chartReference, request), operation)));

    var response =
        requireResponse(support.convert(root, ShowPayload.class, operation), operation, "data");
    requireResponse(response.sections(), operation, "sections");

    if (response.mode() != null && !mode.wireValue().equals(response.mode())) {
      throw runtimeFailure("decodeResponse", operation, "Native show response mode mismatch", null);
    }

    return response;
  }

  private record SearchPayload(List<SearchResultPayload> results) {}

  private record SearchResultPayload(
      String name,
      String version,
      String appVersion,
      String description,
      int score,
      String url,
      String repositoryName,
      String repositoryUrl) {}

  private record ShowPayload(
      String mode,
      String chartRef,
      String chartPath,
      ShowSectionsPayload sections,
      String cliOutput) {}

  private record ShowSectionsPayload(
      String chart, String values, String readme, List<String> crds) {}

  private record TemplatePayload(NativeReleasePayload release, String manifest) {}

  private record LintPayload(
      List<LintMessagePayload> messages, int totalCharts, int chartsTested, int chartsFailed) {}

  private record LintMessagePayload(String severity, String message) {}

  private record PullPayload(String output) {}

  private record PushPayload(String output) {}

  private record PackagePayload(String path) {}

  private record DependencyPayload(String output) {}
}
