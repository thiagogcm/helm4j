package dev.nthings.helm4j.client;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Objects;

import dev.nthings.helm4j.exceptions.HelmException;
import dev.nthings.helm4j.jextract.libhelm4j_h;
import dev.nthings.helm4j.model.SearchResponse;
import dev.nthings.helm4j.model.ShowResponse;
import dev.nthings.helm4j.options.SearchOptions;
import dev.nthings.helm4j.options.ShowOptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Idiomatic Java wrapper over the native libhelm4j bindings generated via jextract.
 *
 * <p>The native layer returns JSON strings; this class turns them into typed DTOs and throws {@link
 * HelmException} on errors.
 */
public final class HelmClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(HelmClient.class);
  private final ObjectMapper mapper;
  private final NativeShowInvoker showChartInvoker;
  private final NativeShowInvoker showValuesInvoker;
  private final NativeShowInvoker showReadmeInvoker;
  private final NativeShowInvoker showAllInvoker;
  private final NativeSearchInvoker searchInvoker;
  private final NativeStringReleaser stringReleaser;

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
    this.stringReleaser = Objects.requireNonNull(stringReleaser, "stringReleaser");
  }

  /** Show chart metadata (equivalent to {@code helm show chart}). */
  public ShowResponse showChart(String chartRef, ShowOptions options) {
    return doShow(chartRef, options, showChartInvoker, "chart");
  }

  /** Show values (equivalent to {@code helm show values}). */
  public ShowResponse showValues(String chartRef, ShowOptions options) {
    return doShow(chartRef, options, showValuesInvoker, "values");
  }

  /** Show README (equivalent to {@code helm show readme}). */
  public ShowResponse showReadme(String chartRef, ShowOptions options) {
    return doShow(chartRef, options, showReadmeInvoker, "readme");
  }

  /** Show all sections (equivalent to {@code helm show all}). */
  public ShowResponse showAll(String chartRef, ShowOptions options) {
    return doShow(chartRef, options, showAllInvoker, "all");
  }

  /** Search chart repositories (equivalent to {@code helm search repo}). */
  public SearchResponse search(SearchOptions options) {
    Objects.requireNonNull(options, "options");
    LOGGER.debug("Running helm search for keyword='{}'", options.keyword());

    try (var arena = Arena.ofConfined()) {
      var opts = arena.allocateFrom(toJson(options));
      var resultPtr = searchInvoker.invoke(opts);
      var response = decodeSearchResponse(resultPtr);
      var resultCount = response.results() == null ? 0 : response.results().size();
      LOGGER.debug("Helm search completed with {} result(s)", resultCount);
      return response;
    }
  }

  private ShowResponse doShow(
      String chartRef, ShowOptions options, NativeShowInvoker invoker, String mode) {
    Objects.requireNonNull(chartRef, "chartRef");
    Objects.requireNonNull(options, "options");
    LOGGER.debug("Running helm show '{}' for chartRef='{}'", mode, chartRef);

    try (var arena = Arena.ofConfined()) {
      var chart = arena.allocateFrom(chartRef);
      var opts = arena.allocateFrom(toJson(options));
      var resultPtr = invoker.invoke(chart, opts);
      var response = decodeShowResponse(resultPtr);
      LOGGER.debug(
          "Helm show '{}' completed for chartRef='{}' (resolvedPath='{}')",
          mode,
          response.chartRef(),
          response.chartPath());
      return response;
    }
  }

  private ShowResponse decodeShowResponse(MemorySegment resultPtr) {
    var json = readAndFree(resultPtr);
    var root = parseTree(json);
    if (root.hasNonNull("error")) {
      var message = root.path("error").asString();
      var stage = root.path("stage").asString(null);
      var mode = root.path("mode").asString(null);
      var chartRef = root.path("chartRef").asString(null);
      var chartPath = root.path("chartPath").asString(null);
      LOGGER.error(
          "Native helm show failure: stage='{}', mode='{}', chartRef='{}', chartPath='{}',"
              + " message='{}'",
          stage,
          mode,
          chartRef,
          chartPath,
          message);
      throw new HelmException(message, stage, mode, chartRef, chartPath);
    }
    return readValue(json, ShowResponse.class);
  }

  private SearchResponse decodeSearchResponse(MemorySegment resultPtr) {
    var json = readAndFree(resultPtr);
    var root = parseTree(json);
    if (root.hasNonNull("error")) {
      var message = root.path("error").asString();
      var stage = root.path("stage").asString(null);
      LOGGER.error("Native helm search failure: stage='{}', message='{}'", stage, message);
      throw new HelmException(message, stage, null, null, null);
    }
    return readValue(json, SearchResponse.class);
  }

  private String readAndFree(MemorySegment ptr) {
    if (ptr == null || ptr.equals(MemorySegment.NULL)) {
      LOGGER.error("Native layer returned a null response pointer");
      return "";
    }
    // Interpret as unbounded C string before freeing.
    var value = ptr.reinterpret(Long.MAX_VALUE).getString(0);
    stringReleaser.free(ptr);
    return value;
  }

  private String toJson(Object value) {
    try {
      return mapper.writeValueAsString(value);
    } catch (JacksonException e) {
      var type = value == null ? "null" : value.getClass().getSimpleName();
      LOGGER.error("Failed to encode options payload for type '{}'", type, e);
      throw new IllegalArgumentException("Failed to encode options", e);
    }
  }

  private JsonNode parseTree(String json) {
    try {
      return mapper.readTree(json);
    } catch (JacksonException e) {
      LOGGER.error(
          "Invalid JSON returned by native layer. Payload preview='{}'", abbreviate(json, 200), e);
      throw new IllegalStateException("Invalid JSON returned by native layer", e);
    }
  }

  private <T> T readValue(String json, Class<T> type) {
    try {
      return mapper.readValue(json, type);
    } catch (JacksonException e) {
      LOGGER.error(
          "Failed to decode native response as type '{}'. Payload preview='{}'",
          type.getSimpleName(),
          abbreviate(json, 200),
          e);
      throw new IllegalStateException("Failed to decode native response", e);
    }
  }

  private static String abbreviate(String value, int maxLength) {
    if (value == null) {
      return "";
    }
    if (value.length() <= maxLength) {
      return value;
    }
    return value.substring(0, maxLength) + "...";
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
