package dev.nthings.helm4j.client;

import java.util.Objects;

import dev.nthings.helm4j.bindings.NativeHelmBindings;
import dev.nthings.helm4j.bindings.NativePayloadCodec;
import dev.nthings.helm4j.bindings.NativePayloadMapper;
import dev.nthings.helm4j.bindings.NativeShowPayload;
import dev.nthings.helm4j.model.ChartCrds;
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
 * <p>This class hides native payload details behind typed Java options and results.
 */
public final class HelmClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(HelmClient.class);

  private final ObjectMapper mapper;
  private final NativeHelmBindings bindings;
  private final NativePayloadCodec payloadCodec;

  HelmClient(ObjectMapper mapper, NativeHelmBindings bindings) {
    this.mapper = Objects.requireNonNull(mapper, "mapper");
    this.bindings = Objects.requireNonNull(bindings, "bindings");
    this.payloadCodec = new NativePayloadCodec(this.mapper);
  }

  /** Show chart metadata (equivalent to {@code helm show chart}). */
  public ChartMetadata showChart(String chartReference) {
    return showChart(chartReference, ShowOptions.defaults());
  }

  /** Show chart metadata (equivalent to {@code helm show chart}). */
  public ChartMetadata showChart(String chartReference, ShowOptions options) {
    var payload = runShow(chartReference, options, ShowMode.CHART);
    return NativePayloadMapper.toChartMetadata(payload, chartReference);
  }

  /** Show values (equivalent to {@code helm show values}). */
  public ChartValues showValues(String chartReference) {
    return showValues(chartReference, ShowOptions.defaults());
  }

  /** Show values (equivalent to {@code helm show values}). */
  public ChartValues showValues(String chartReference, ShowOptions options) {
    var payload = runShow(chartReference, options, ShowMode.VALUES);
    return NativePayloadMapper.toChartValues(payload, chartReference);
  }

  /** Show README (equivalent to {@code helm show readme}). */
  public ChartReadme showReadme(String chartReference) {
    return showReadme(chartReference, ShowOptions.defaults());
  }

  /** Show README (equivalent to {@code helm show readme}). */
  public ChartReadme showReadme(String chartReference, ShowOptions options) {
    var payload = runShow(chartReference, options, ShowMode.README);
    return NativePayloadMapper.toChartReadme(payload, chartReference);
  }

  /** Show all chart sections (equivalent to {@code helm show all}). */
  public ChartDetails showAll(String chartReference) {
    return showAll(chartReference, ShowOptions.defaults());
  }

  /** Show all chart sections (equivalent to {@code helm show all}). */
  public ChartDetails showAll(String chartReference, ShowOptions options) {
    var payload = runShow(chartReference, options, ShowMode.ALL);
    return NativePayloadMapper.toChartDetails(payload, chartReference);
  }

  /** Show CRDs (equivalent to {@code helm show crds}). */
  public ChartCrds showCrds(String chartReference) {
    return showCrds(chartReference, ShowOptions.defaults());
  }

  /** Show CRDs (equivalent to {@code helm show crds}). */
  public ChartCrds showCrds(String chartReference, ShowOptions options) {
    var payload = runShow(chartReference, options, ShowMode.CRDS);
    return NativePayloadMapper.toChartCrds(payload, chartReference);
  }

  /** Search chart repositories (equivalent to {@code helm search repo}). */
  public SearchResultSet search(String query) {
    return search(SearchOptions.builder().query(query).build());
  }

  /** Search chart repositories (equivalent to {@code helm search repo}). */
  public SearchResultSet search(SearchOptions options) {
    Objects.requireNonNull(options, "options");
    LOGGER.debug("Running helm search for query='{}'", options.query());

    var payload = NativePayloadMapper.toNativeSearchOptions(options);
    var optionsJson = payloadCodec.toJson(payload);
    var responseJson = bindings.search(optionsJson);
    var nativeResponse = payloadCodec.decodeSearchResponse(responseJson);
    var response = NativePayloadMapper.toSearchResultSet(nativeResponse);

    LOGGER.debug("Helm search completed with {} result(s)", response.size());
    return response;
  }

  private NativeShowPayload runShow(
      String chartReference, ShowOptions options, ShowMode expectedMode) {
    Objects.requireNonNull(chartReference, "chartReference");
    Objects.requireNonNull(options, "options");

    LOGGER.debug(
        "Running helm show '{}' for chartReference='{}'", expectedMode.toJson(), chartReference);

    var payload = NativePayloadMapper.toNativeShowOptions(options);
    var optionsJson = payloadCodec.toJson(payload);
    var responseJson = bindings.show(expectedMode, chartReference, optionsJson);
    var response = payloadCodec.decodeShowResponse(responseJson);
    NativePayloadMapper.ensureMode(expectedMode, response.mode());

    LOGGER.debug(
        "Helm show '{}' completed for chartReference='{}' (resolvedPath='{}')",
        expectedMode.toJson(),
        NativePayloadMapper.chartReferenceOf(response, chartReference),
        NativePayloadMapper.resolvedPathOf(response));
    return response;
  }
}
