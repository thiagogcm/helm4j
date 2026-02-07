package dev.nthings.helm4j.bindings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import dev.nthings.helm4j.exceptions.HelmException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.ObjectWriter;

public final class NativePayloadCodec {

  private static final Logger LOGGER = LoggerFactory.getLogger(NativePayloadCodec.class);

  private final ObjectWriter writer;
  private final ObjectReader showEnvelopeReader;
  private final ObjectReader searchEnvelopeReader;

  public NativePayloadCodec(ObjectMapper mapper) {
    this.writer = mapper.writer();
    this.showEnvelopeReader = mapper.readerFor(NativeShowEnvelope.class);
    this.searchEnvelopeReader = mapper.readerFor(NativeSearchEnvelope.class);
  }

  public String toJson(Object value) {
    try {
      return writer.writeValueAsString(value);
    } catch (JacksonException e) {
      var type = value == null ? "null" : value.getClass().getSimpleName();
      LOGGER.error("Failed to encode options payload for type '{}'", type, e);
      throw new IllegalArgumentException("Failed to encode options", e);
    }
  }

  public NativeShowPayload decodeShowResponse(String json) {
    var envelope = readEnvelope(json, showEnvelopeReader, NativeShowEnvelope.class);
    if (envelope.error() != null) {
      var message = envelope.error();
      var stage = normalizeText(envelope.stage());
      var mode = normalizeText(envelope.mode());
      var chartRef = normalizeText(envelope.chartRef());
      var chartPath = normalizeText(envelope.chartPath());
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
    return new NativeShowPayload(
        envelope.mode(),
        envelope.chartRef(),
        envelope.chartPath(),
        envelope.sections(),
        envelope.cliOutput());
  }

  public NativeSearchPayload decodeSearchResponse(String json) {
    var envelope = readEnvelope(json, searchEnvelopeReader, NativeSearchEnvelope.class);
    if (envelope.error() != null) {
      var message = envelope.error();
      var stage = normalizeText(envelope.stage());
      LOGGER.error("Native helm search failure: stage='{}', message='{}'", stage, message);
      throw new HelmException(message, stage, null, null, null);
    }
    return new NativeSearchPayload(envelope.results());
  }

  private <T> T readEnvelope(String json, ObjectReader reader, Class<T> type) {
    if (json == null || json.isBlank()) {
      LOGGER.error("Empty payload returned by native layer");
      throw new IllegalStateException("Empty payload returned by native layer");
    }

    try {
      return reader.readValue(json);
    } catch (JacksonException e) {
      LOGGER.error(
          "Failed to decode native response as type '{}'. Payload preview='{}'",
          type.getSimpleName(),
          abbreviate(json, 200),
          e);
      throw new IllegalStateException("Invalid JSON returned by native layer", e);
    }
  }

  private static String normalizeText(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value;
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

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record NativeShowEnvelope(
      @JsonProperty("mode") String mode,
      @JsonProperty("chartRef") String chartRef,
      @JsonProperty("chartPath") String chartPath,
      @JsonProperty("sections") NativeShowSections sections,
      @JsonProperty("cliOutput") String cliOutput,
      @JsonProperty("error") String error,
      @JsonProperty("stage") String stage) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record NativeSearchEnvelope(
      @JsonProperty("results") java.util.List<NativeSearchResult> results,
      @JsonProperty("error") String error,
      @JsonProperty("stage") String stage) {}
}
