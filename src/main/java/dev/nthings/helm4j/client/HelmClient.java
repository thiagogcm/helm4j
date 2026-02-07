package dev.nthings.helm4j.client;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import dev.nthings.helm4j.exceptions.HelmException;
import dev.nthings.helm4j.jextract.libhelm4j_h;
import dev.nthings.helm4j.model.ChartDetails;
import dev.nthings.helm4j.model.ChartMetadata;
import dev.nthings.helm4j.model.ChartReadme;
import dev.nthings.helm4j.model.ChartSummary;
import dev.nthings.helm4j.model.ChartValues;
import dev.nthings.helm4j.model.SearchResultSet;
import dev.nthings.helm4j.model.ShowMode;
import dev.nthings.helm4j.options.SearchOptions;
import dev.nthings.helm4j.options.ShowOptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
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
  public ChartMetadata showChart(String chartReference) {
    return showChart(chartReference, ShowOptions.defaults());
  }

  /** Show chart metadata (equivalent to {@code helm show chart}). */
  public ChartMetadata showChart(String chartReference, ShowOptions options) {
    var payload = runShow(chartReference, options, showChartInvoker, ShowMode.CHART);
    var sections = sectionsOrEmpty(payload.sections());
    return new ChartMetadata(
        chartReferenceOf(payload, chartReference),
        resolvedPathOf(payload),
        textOrEmpty(sections.chart()),
        textOrEmpty(payload.cliOutput()));
  }

  /** Show values (equivalent to {@code helm show values}). */
  public ChartValues showValues(String chartReference) {
    return showValues(chartReference, ShowOptions.defaults());
  }

  /** Show values (equivalent to {@code helm show values}). */
  public ChartValues showValues(String chartReference, ShowOptions options) {
    var payload = runShow(chartReference, options, showValuesInvoker, ShowMode.VALUES);
    var sections = sectionsOrEmpty(payload.sections());
    return new ChartValues(
        chartReferenceOf(payload, chartReference),
        resolvedPathOf(payload),
        textOrEmpty(sections.values()),
        textOrEmpty(payload.cliOutput()));
  }

  /** Show README (equivalent to {@code helm show readme}). */
  public ChartReadme showReadme(String chartReference) {
    return showReadme(chartReference, ShowOptions.defaults());
  }

  /** Show README (equivalent to {@code helm show readme}). */
  public ChartReadme showReadme(String chartReference, ShowOptions options) {
    var payload = runShow(chartReference, options, showReadmeInvoker, ShowMode.README);
    var sections = sectionsOrEmpty(payload.sections());
    return new ChartReadme(
        chartReferenceOf(payload, chartReference),
        resolvedPathOf(payload),
        textOrEmpty(sections.readme()),
        textOrEmpty(payload.cliOutput()));
  }

  /** Show all chart sections (equivalent to {@code helm show all}). */
  public ChartDetails showAll(String chartReference) {
    return showAll(chartReference, ShowOptions.defaults());
  }

  /** Show all chart sections (equivalent to {@code helm show all}). */
  public ChartDetails showAll(String chartReference, ShowOptions options) {
    var payload = runShow(chartReference, options, showAllInvoker, ShowMode.ALL);
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

  /** Search chart repositories (equivalent to {@code helm search repo}). */
  public SearchResultSet search(String query) {
    return search(SearchOptions.builder().query(query).build());
  }

  /** Search chart repositories (equivalent to {@code helm search repo}). */
  public SearchResultSet search(SearchOptions options) {
    Objects.requireNonNull(options, "options");
    LOGGER.debug("Running helm search for query='{}'", options.query());

    try (var arena = Arena.ofConfined()) {
      var payload = nativeSearchOptions(options);
      var opts = arena.allocateFrom(toJson(payload));
      var resultPtr = searchInvoker.invoke(opts);
      var nativeResponse = decodeSearchResponse(resultPtr);
      var charts =
          listOrEmpty(nativeResponse.results()).stream()
              .map(
                  result ->
                      new ChartSummary(
                          textOrEmpty(result.name()),
                          textOrEmpty(result.version()),
                          textOrEmpty(result.appVersion()),
                          textOrEmpty(result.description()),
                          intOrZero(result.score())))
              .toList();

      LOGGER.debug("Helm search completed with {} result(s)", charts.size());
      return new SearchResultSet(charts);
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
      var opts = arena.allocateFrom(toJson(nativeShowOptions(options)));
      var resultPtr = invoker.invoke(chart, opts);
      var payload = decodeShowResponse(resultPtr);
      ensureMode(expectedMode, payload.mode());
      LOGGER.debug(
          "Helm show '{}' completed for chartReference='{}' (resolvedPath='{}')",
          expectedMode.toJson(),
          chartReferenceOf(payload, chartReference),
          resolvedPathOf(payload));
      return payload;
    }
  }

  private NativeShowPayload decodeShowResponse(MemorySegment resultPtr) {
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
    return readValue(json, NativeShowPayload.class);
  }

  private NativeSearchPayload decodeSearchResponse(MemorySegment resultPtr) {
    var json = readAndFree(resultPtr);
    var root = parseTree(json);
    if (root.hasNonNull("error")) {
      var message = root.path("error").asString();
      var stage = root.path("stage").asString(null);
      LOGGER.error("Native helm search failure: stage='{}', message='{}'", stage, message);
      throw new HelmException(message, stage, null, null, null);
    }
    return readValue(json, NativeSearchPayload.class);
  }

  private String readAndFree(MemorySegment ptr) {
    if (ptr == null || ptr.equals(MemorySegment.NULL)) {
      LOGGER.error("Native layer returned a null response pointer");
      return "";
    }

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

  private static void ensureMode(ShowMode expectedMode, String rawMode) {
    var actualMode = ShowMode.fromString(rawMode);
    if (actualMode != expectedMode) {
      throw new IllegalStateException(
          "Native response mode mismatch: expected="
              + expectedMode.toJson()
              + ", actual="
              + textOrEmpty(rawMode));
    }
  }

  private static NativeShowOptions nativeShowOptions(ShowOptions options) {
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

  private static NativeSearchOptions nativeSearchOptions(SearchOptions options) {
    return new NativeSearchOptions(
        options.query(),
        options.regularExpression(),
        options.includeAllVersions(),
        options.includePreReleaseVersions(),
        options.versionConstraint(),
        options.failIfNoResults());
  }

  private static NativeShowSections sectionsOrEmpty(NativeShowSections value) {
    return value == null ? new NativeShowSections(null, null, null, null) : value;
  }

  private static String chartReferenceOf(NativeShowPayload payload, String fallback) {
    return payload.chartRef() == null ? fallback : payload.chartRef();
  }

  private static String resolvedPathOf(NativeShowPayload payload) {
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

  private static String abbreviate(String value, int maxLength) {
    if (value == null) {
      return "";
    }
    if (value.length() <= maxLength) {
      return value;
    }
    return value.substring(0, maxLength) + "...";
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private record NativeShowOptions(
      @JsonProperty("version") String version,
      @JsonProperty("repo") String repositoryUrl,
      @JsonProperty("username") String username,
      @JsonProperty("password") String password,
      @JsonProperty("plainHttp") Boolean plainHttp,
      @JsonProperty("insecureSkipTlsVerify") Boolean insecureSkipTlsVerification,
      @JsonProperty("keyring") String keyringPath,
      @JsonProperty("certFile") String certificateFile,
      @JsonProperty("keyFile") String keyFile,
      @JsonProperty("caFile") String certificateAuthorityFile,
      @JsonProperty("passCredentialsAll") Boolean passCredentialsToAllHosts,
      @JsonProperty("verify") Boolean verifySignatures,
      @JsonProperty("devel") Boolean includePreReleaseVersions,
      @JsonProperty("jsonpath") String valuesJsonPath) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private record NativeSearchOptions(
      @JsonProperty("keyword") String query,
      @JsonProperty("regexp") Boolean regularExpression,
      @JsonProperty("versions") Boolean includeAllVersions,
      @JsonProperty("devel") Boolean includePreReleaseVersions,
      @JsonProperty("version") String versionConstraint,
      @JsonProperty("failOnNoResult") Boolean failIfNoResults) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record NativeShowPayload(
      @JsonProperty("mode") String mode,
      @JsonProperty("chartRef") String chartRef,
      @JsonProperty("chartPath") String chartPath,
      @JsonProperty("sections") NativeShowSections sections,
      @JsonProperty("cliOutput") String cliOutput) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record NativeShowSections(
      @JsonProperty("chart") String chart,
      @JsonProperty("values") String values,
      @JsonProperty("readme") String readme,
      @JsonProperty("crds") List<String> crds) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record NativeSearchPayload(@JsonProperty("results") List<NativeSearchResult> results) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record NativeSearchResult(
      @JsonProperty("name") String name,
      @JsonProperty("version") String version,
      @JsonProperty("appVersion") String appVersion,
      @JsonProperty("description") String description,
      @JsonProperty("score") Integer score) {}

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
