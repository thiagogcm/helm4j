package dev.nthings.helm4j.bindings;

import java.util.List;
import java.util.Objects;

import dev.nthings.helm4j.model.ChartCrds;
import dev.nthings.helm4j.model.ChartDetails;
import dev.nthings.helm4j.model.ChartMetadata;
import dev.nthings.helm4j.model.ChartReadme;
import dev.nthings.helm4j.model.ChartSummary;
import dev.nthings.helm4j.model.ChartValues;
import dev.nthings.helm4j.model.RepoAddResult;
import dev.nthings.helm4j.model.RepoListResult;
import dev.nthings.helm4j.model.RepoRemoveResult;
import dev.nthings.helm4j.model.RepoSummary;
import dev.nthings.helm4j.model.RepoUpdateEntry;
import dev.nthings.helm4j.model.RepoUpdateResult;
import dev.nthings.helm4j.model.SearchResultSet;
import dev.nthings.helm4j.model.ShowMode;
import dev.nthings.helm4j.options.RepoAddOptions;
import dev.nthings.helm4j.options.RepoListOptions;
import dev.nthings.helm4j.options.RepoRemoveOptions;
import dev.nthings.helm4j.options.RepoUpdateOptions;
import dev.nthings.helm4j.options.SearchOptions;
import dev.nthings.helm4j.options.ShowOptions;

public final class NativePayloadMapper {

  private NativePayloadMapper() {}

  public static NativeShowOptions toNativeShowOptions(ShowOptions options) {
    return new NativeShowOptions(
        options.version(),
        options.repositoryUrl(),
        options.username(),
        options.password(),
        options.plainHttp(),
        options.insecureSkipTlsVerification(),
        options.keyringPath(),
        options.certificateFile(),
        options.keyFile(),
        options.certificateAuthorityFile(),
        options.passCredentialsToAllHosts(),
        options.verifySignatures(),
        options.includePreReleaseVersions(),
        options.valuesJsonPath());
  }

  public static NativeSearchOptions toNativeSearchOptions(SearchOptions options) {
    return new NativeSearchOptions(
        options.query(),
        options.regularExpression(),
        options.includeAllVersions(),
        options.includePreReleaseVersions(),
        options.versionConstraint(),
        options.failIfNoResults());
  }

  public static NativeRepoAddOptions toNativeRepoAddOptions(RepoAddOptions options) {
    return new NativeRepoAddOptions(
        options.name(),
        options.url(),
        options.username(),
        options.password(),
        options.certificateFile(),
        options.keyFile(),
        options.certificateAuthorityFile(),
        options.insecureSkipTlsVerification(),
        options.passCredentialsToAllHosts(),
        options.forceUpdate());
  }

  public static NativeRepoUpdateOptions toNativeRepoUpdateOptions(RepoUpdateOptions options) {
    return new NativeRepoUpdateOptions(listOrEmpty(options.names()));
  }

  public static NativeRepoListOptions toNativeRepoListOptions(RepoListOptions options) {
    Objects.requireNonNull(options, "options");
    return new NativeRepoListOptions();
  }

  public static NativeRepoRemoveOptions toNativeRepoRemoveOptions(RepoRemoveOptions options) {
    return new NativeRepoRemoveOptions(listOrEmpty(options.names()));
  }

  public static ChartMetadata toChartMetadata(NativeShowPayload payload, String chartReference) {
    var sections = sectionsOrEmpty(payload.sections());
    return new ChartMetadata(
        chartReferenceOf(payload, chartReference),
        resolvedPathOf(payload),
        textOrEmpty(sections.chart()),
        textOrEmpty(payload.cliOutput()));
  }

  public static ChartValues toChartValues(NativeShowPayload payload, String chartReference) {
    var sections = sectionsOrEmpty(payload.sections());
    return new ChartValues(
        chartReferenceOf(payload, chartReference),
        resolvedPathOf(payload),
        textOrEmpty(sections.values()),
        textOrEmpty(payload.cliOutput()));
  }

  public static ChartReadme toChartReadme(NativeShowPayload payload, String chartReference) {
    var sections = sectionsOrEmpty(payload.sections());
    return new ChartReadme(
        chartReferenceOf(payload, chartReference),
        resolvedPathOf(payload),
        textOrEmpty(sections.readme()),
        textOrEmpty(payload.cliOutput()));
  }

  public static ChartDetails toChartDetails(NativeShowPayload payload, String chartReference) {
    var sections = sectionsOrEmpty(payload.sections());
    return new ChartDetails(
        chartReferenceOf(payload, chartReference),
        resolvedPathOf(payload),
        textOrEmpty(sections.chart()),
        textOrEmpty(sections.values()),
        textOrEmpty(sections.readme()),
        listOrEmpty(sections.crds()),
        textOrEmpty(payload.cliOutput()));
  }

  public static ChartCrds toChartCrds(NativeShowPayload payload, String chartReference) {
    var sections = sectionsOrEmpty(payload.sections());
    return new ChartCrds(
        chartReferenceOf(payload, chartReference),
        resolvedPathOf(payload),
        listOrEmpty(sections.crds()),
        textOrEmpty(payload.cliOutput()));
  }

  public static SearchResultSet toSearchResultSet(NativeSearchPayload payload) {
    var charts =
        listOrEmpty(payload.results()).stream()
            .map(
                result ->
                    new ChartSummary(
                        textOrEmpty(result.name()),
                        textOrEmpty(result.version()),
                        textOrEmpty(result.appVersion()),
                        textOrEmpty(result.description()),
                        intOrZero(result.score())))
            .toList();

    return new SearchResultSet(charts);
  }

  public static RepoAddResult toRepoAddResult(NativeRepoAddPayload payload) {
    return new RepoAddResult(textOrEmpty(payload.name()), textOrEmpty(payload.url()));
  }

  public static RepoUpdateResult toRepoUpdateResult(NativeRepoUpdatePayload payload) {
    var repositories =
        listOrEmpty(payload.repositories()).stream()
            .map(
                entry ->
                    new RepoUpdateEntry(textOrEmpty(entry.name()), textOrEmpty(entry.status())))
            .toList();
    return new RepoUpdateResult(repositories);
  }

  public static RepoListResult toRepoListResult(NativeRepoListPayload payload) {
    var repositories =
        listOrEmpty(payload.repositories()).stream()
            .map(entry -> new RepoSummary(textOrEmpty(entry.name()), textOrEmpty(entry.url())))
            .toList();
    return new RepoListResult(repositories);
  }

  public static RepoRemoveResult toRepoRemoveResult(NativeRepoRemovePayload payload) {
    var removed =
        listOrEmpty(payload.removed()).stream().map(NativePayloadMapper::textOrEmpty).toList();
    return new RepoRemoveResult(removed);
  }

  public static void ensureMode(ShowMode expectedMode, String rawMode) {
    var actualMode = ShowMode.fromString(rawMode);
    if (actualMode == null || actualMode != expectedMode) {
      throw new IllegalStateException(
          "Native response mode mismatch: expected="
              + expectedMode.toJson()
              + ", actual="
              + textOrEmpty(rawMode));
    }
  }

  private static NativeShowSections sectionsOrEmpty(NativeShowSections value) {
    return value == null ? new NativeShowSections(null, null, null, null) : value;
  }

  public static String chartReferenceOf(NativeShowPayload payload, String fallback) {
    return payload.chartRef() == null ? fallback : payload.chartRef();
  }

  public static String resolvedPathOf(NativeShowPayload payload) {
    return textOrEmpty(payload.chartPath());
  }

  private static int intOrZero(Integer value) {
    return value == null ? 0 : value;
  }

  private static String textOrEmpty(String value) {
    return value == null ? "" : value;
  }

  private static <T> List<T> listOrEmpty(List<T> value) {
    return value == null ? List.of() : List.copyOf(value);
  }
}
