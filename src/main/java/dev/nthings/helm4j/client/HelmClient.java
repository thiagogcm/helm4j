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

  private final ObjectMapper mapper;

  HelmClient(ObjectMapper mapper) {
    this.mapper = Objects.requireNonNull(mapper, "mapper");
  }

  /** Show chart metadata (equivalent to {@code helm show chart}). */
  public ShowResponse showChart(String chartRef, ShowOptions options) {
    return doShow(chartRef, options, libhelm4j_h::HelmShowChart);
  }

  /** Show values (equivalent to {@code helm show values}). */
  public ShowResponse showValues(String chartRef, ShowOptions options) {
    return doShow(chartRef, options, libhelm4j_h::HelmShowValues);
  }

  /** Show README (equivalent to {@code helm show readme}). */
  public ShowResponse showReadme(String chartRef, ShowOptions options) {
    return doShow(chartRef, options, libhelm4j_h::HelmShowReadme);
  }

  /** Show all sections (equivalent to {@code helm show all}). */
  public ShowResponse showAll(String chartRef, ShowOptions options) {
    return doShow(chartRef, options, libhelm4j_h::HelmShowAll);
  }

  /** Search chart repositories (equivalent to {@code helm search repo}). */
  public SearchResponse search(SearchOptions options) {
    Objects.requireNonNull(options, "options");

    try (var arena = Arena.ofConfined()) {
      var opts = arena.allocateFrom(toJson(options));
      var resultPtr = libhelm4j_h.HelmSearch(opts);
      return decodeSearchResponse(resultPtr);
    }
  }

  private ShowResponse doShow(String chartRef, ShowOptions options, NativeShowInvoker invoker) {
    Objects.requireNonNull(chartRef, "chartRef");
    Objects.requireNonNull(options, "options");

    try (var arena = Arena.ofConfined()) {
      var chart = arena.allocateFrom(chartRef);
      var opts = arena.allocateFrom(toJson(options));
      var resultPtr = invoker.invoke(chart, opts);
      return decodeShowResponse(resultPtr);
    }
  }

  private ShowResponse decodeShowResponse(MemorySegment resultPtr) {
    var json = readAndFree(resultPtr);
    var root = parseTree(json);
    if (root.hasNonNull("error")) {
      throw new HelmException(
          root.path("error").asString(),
          root.path("stage").asString(null),
          root.path("mode").asString(null),
          root.path("chartRef").asString(null),
          root.path("chartPath").asString(null));
    }
    return readValue(json, ShowResponse.class);
  }

  private SearchResponse decodeSearchResponse(MemorySegment resultPtr) {
    var json = readAndFree(resultPtr);
    var root = parseTree(json);
    if (root.hasNonNull("error")) {
      throw new HelmException(
          root.path("error").asString(), root.path("stage").asString(null), null, null, null);
    }
    return readValue(json, SearchResponse.class);
  }

  private static String readAndFree(MemorySegment ptr) {
    if (ptr == null || ptr.equals(MemorySegment.NULL)) {
      return "";
    }
    // Interpret as unbounded C string before freeing.
    var value = ptr.reinterpret(Long.MAX_VALUE).getString(0);
    libhelm4j_h.FreeString(ptr);
    return value;
  }

  private String toJson(Object value) {
    try {
      return mapper.writeValueAsString(value);
    } catch (JacksonException e) {
      throw new IllegalArgumentException("Failed to encode options", e);
    }
  }

  private JsonNode parseTree(String json) {
    try {
      return mapper.readTree(json);
    } catch (Exception e) {
      throw new IllegalStateException("Invalid JSON returned by native layer", e);
    }
  }

  private <T> T readValue(String json, Class<T> type) {
    try {
      return mapper.readValue(json, type);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to decode native response", e);
    }
  }

  @FunctionalInterface
  private interface NativeShowInvoker {
    MemorySegment invoke(MemorySegment chartRef, MemorySegment options);
  }
}
