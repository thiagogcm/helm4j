package dev.nthings.helm4j.client;

import java.util.List;

import dev.nthings.helm4j.model.ChartDetails;
import dev.nthings.helm4j.model.ChartMetadata;
import dev.nthings.helm4j.model.ChartReadme;
import dev.nthings.helm4j.model.ChartSummary;
import dev.nthings.helm4j.model.ChartValues;
import dev.nthings.helm4j.model.SearchResultSet;
import dev.nthings.helm4j.model.ShowMode;
import dev.nthings.helm4j.options.SearchOptions;
import dev.nthings.helm4j.options.ShowOptions;

final class NativePayloadMapper {

  private NativePayloadMapper() {}

  static NativeShowOptions toNativeShowOptions(ShowOptions options) {
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

  static NativeSearchOptions toNativeSearchOptions(SearchOptions options) {
    return new NativeSearchOptions(
        options.query(),
        options.regularExpression(),
        options.includeAllVersions(),
        options.includePreReleaseVersions(),
        options.versionConstraint(),
        options.failIfNoResults());
  }

  static ChartMetadata toChartMetadata(NativeShowPayload payload, String chartReference) {
    var sections = sectionsOrEmpty(payload.sections());
    return new ChartMetadata(
        chartReferenceOf(payload, chartReference),
        resolvedPathOf(payload),
        textOrEmpty(sections.chart()),
        textOrEmpty(payload.cliOutput()));
  }

  static ChartValues toChartValues(NativeShowPayload payload, String chartReference) {
    var sections = sectionsOrEmpty(payload.sections());
    return new ChartValues(
        chartReferenceOf(payload, chartReference),
        resolvedPathOf(payload),
        textOrEmpty(sections.values()),
        textOrEmpty(payload.cliOutput()));
  }

  static ChartReadme toChartReadme(NativeShowPayload payload, String chartReference) {
    var sections = sectionsOrEmpty(payload.sections());
    return new ChartReadme(
        chartReferenceOf(payload, chartReference),
        resolvedPathOf(payload),
        textOrEmpty(sections.readme()),
        textOrEmpty(payload.cliOutput()));
  }

  static ChartDetails toChartDetails(NativeShowPayload payload, String chartReference) {
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

  static SearchResultSet toSearchResultSet(NativeSearchPayload payload) {
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

  static void ensureMode(ShowMode expectedMode, String rawMode) {
    var actualMode = ShowMode.fromString(rawMode);
    if (actualMode != expectedMode) {
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

  static String chartReferenceOf(NativeShowPayload payload, String fallback) {
    return payload.chartRef() == null ? fallback : payload.chartRef();
  }

  static String resolvedPathOf(NativeShowPayload payload) {
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
