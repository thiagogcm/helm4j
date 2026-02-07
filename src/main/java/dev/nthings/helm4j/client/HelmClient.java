package dev.nthings.helm4j.client;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Objects;

import dev.nthings.helm4j.jextract.libhelm4j_h;
import dev.nthings.helm4j.model.ChartDetails;
import dev.nthings.helm4j.model.ChartMetadata;
import dev.nthings.helm4j.model.ChartReadme;
import dev.nthings.helm4j.model.ChartValues;
import dev.nthings.helm4j.model.SearchResultSet;
import dev.nthings.helm4j.model.ShowMode;
import dev.nthings.helm4j.options.SearchOptions;
import dev.nthings.helm4j.options.ShowOptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.jackson.databind.ObjectMapper;

/**
 * Consumer-facing Java API for Helm operations backed by native bindings.
 *
 * <p>This class hides FFM and native payload details behind typed Java options and results.
 */
public final class HelmClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(HelmClient.class);

  private final ObjectMapper mapper;
  private final NativeShowInvoker showChartInvoker;
  private final NativeShowInvoker showValuesInvoker;
  private final NativeShowInvoker showReadmeInvoker;
  private final NativeShowInvoker showAllInvoker;
  private final NativeSearchInvoker searchInvoker;
  private final NativePayloadCodec payloadCodec;

  HelmClient(ObjectMapper mapper) {
    this(
        mapper,
        (chartRef, options) -> libhelm4j_h.HelmShowChart(chartRef, options),
        (chartRef, options) -> libhelm4j_h.HelmShowValues(chartRef, options),
        (chartRef, options) -> libhelm4j_h.HelmShowReadme(chartRef, options),
        (chartRef, options) -> libhelm4j_h.HelmShowAll(chartRef, options),
        options -> libhelm4j_h.HelmSearch(options),
        libhelm4j_h::FreeString);
  }

  HelmClient(
      ObjectMapper mapper,
      NativeShowInvoker showChartInvoker,
      NativeShowInvoker showValuesInvoker,
      NativeShowInvoker showReadmeInvoker,
      NativeShowInvoker showAllInvoker,
      NativeSearchInvoker searchInvoker,
      NativeStringReleaser stringReleaser) {
    this.mapper = Objects.requireNonNull(mapper, "mapper");
    this.showChartInvoker = Objects.requireNonNull(showChartInvoker, "showChartInvoker");
    this.showValuesInvoker = Objects.requireNonNull(showValuesInvoker, "showValuesInvoker");
    this.showReadmeInvoker = Objects.requireNonNull(showReadmeInvoker, "showReadmeInvoker");
    this.showAllInvoker = Objects.requireNonNull(showAllInvoker, "showAllInvoker");
    this.searchInvoker = Objects.requireNonNull(searchInvoker, "searchInvoker");
    this.payloadCodec =
        new NativePayloadCodec(
            this.mapper, Objects.requireNonNull(stringReleaser, "stringReleaser"));
  }

  /** Show chart metadata (equivalent to {@code helm show chart}). */
  public ChartMetadata showChart(String chartReference) {
    return showChart(chartReference, ShowOptions.defaults());
  }

  /** Show chart metadata (equivalent to {@code helm show chart}). */
  public ChartMetadata showChart(String chartReference, ShowOptions options) {
    var payload = runShow(chartReference, options, showChartInvoker, ShowMode.CHART);
    return NativePayloadMapper.toChartMetadata(payload, chartReference);
  }

  /** Show values (equivalent to {@code helm show values}). */
  public ChartValues showValues(String chartReference) {
    return showValues(chartReference, ShowOptions.defaults());
  }

  /** Show values (equivalent to {@code helm show values}). */
  public ChartValues showValues(String chartReference, ShowOptions options) {
    var payload = runShow(chartReference, options, showValuesInvoker, ShowMode.VALUES);
    return NativePayloadMapper.toChartValues(payload, chartReference);
  }

  /** Show README (equivalent to {@code helm show readme}). */
  public ChartReadme showReadme(String chartReference) {
    return showReadme(chartReference, ShowOptions.defaults());
  }

  /** Show README (equivalent to {@code helm show readme}). */
  public ChartReadme showReadme(String chartReference, ShowOptions options) {
    var payload = runShow(chartReference, options, showReadmeInvoker, ShowMode.README);
    return NativePayloadMapper.toChartReadme(payload, chartReference);
  }

  /** Show all chart sections (equivalent to {@code helm show all}). */
  public ChartDetails showAll(String chartReference) {
    return showAll(chartReference, ShowOptions.defaults());
  }

  /** Show all chart sections (equivalent to {@code helm show all}). */
  public ChartDetails showAll(String chartReference, ShowOptions options) {
    var payload = runShow(chartReference, options, showAllInvoker, ShowMode.ALL);
    return NativePayloadMapper.toChartDetails(payload, chartReference);
  }

  /** Search chart repositories (equivalent to {@code helm search repo}). */
  public SearchResultSet search(String query) {
    return search(SearchOptions.builder().query(query).build());
  }

  /** Search chart repositories (equivalent to {@code helm search repo}). */
  public SearchResultSet search(SearchOptions options) {
    Objects.requireNonNull(options, "options");
    LOGGER.debug("Running helm search for query='{}'", options.query());

    try (var arena = Arena.ofConfined()) {
      var payload = NativePayloadMapper.toNativeSearchOptions(options);
      var nativeOptions = arena.allocateFrom(payloadCodec.toJson(payload));
      var resultPtr = searchInvoker.invoke(nativeOptions);
      var nativeResponse = payloadCodec.decodeSearchResponse(resultPtr);
      var response = NativePayloadMapper.toSearchResultSet(nativeResponse);

      LOGGER.debug("Helm search completed with {} result(s)", response.size());
      return response;
    }
  }

  private NativeShowPayload runShow(
      String chartReference,
      ShowOptions options,
      NativeShowInvoker invoker,
      ShowMode expectedMode) {
    Objects.requireNonNull(chartReference, "chartReference");
    Objects.requireNonNull(options, "options");

    LOGGER.debug(
        "Running helm show '{}' for chartReference='{}'", expectedMode.toJson(), chartReference);

    try (var arena = Arena.ofConfined()) {
      var chart = arena.allocateFrom(chartReference);
      var payload = NativePayloadMapper.toNativeShowOptions(options);
      var nativeOptions = arena.allocateFrom(payloadCodec.toJson(payload));
      var resultPtr = invoker.invoke(chart, nativeOptions);
      var response = payloadCodec.decodeShowResponse(resultPtr);
      NativePayloadMapper.ensureMode(expectedMode, response.mode());

      LOGGER.debug(
          "Helm show '{}' completed for chartReference='{}' (resolvedPath='{}')",
          expectedMode.toJson(),
          NativePayloadMapper.chartReferenceOf(response, chartReference),
          NativePayloadMapper.resolvedPathOf(response));
      return response;
    }
  }

  @FunctionalInterface
  interface NativeShowInvoker {
    MemorySegment invoke(MemorySegment chartRef, MemorySegment options);
  }

  @FunctionalInterface
  interface NativeSearchInvoker {
    MemorySegment invoke(MemorySegment options);
  }

  @FunctionalInterface
  interface NativeStringReleaser {
    void free(MemorySegment pointer);
  }
}
