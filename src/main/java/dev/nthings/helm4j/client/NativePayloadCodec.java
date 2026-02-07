package dev.nthings.helm4j.client;

import java.lang.foreign.MemorySegment;

import dev.nthings.helm4j.exceptions.HelmException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

final class NativePayloadCodec {

  private static final Logger LOGGER = LoggerFactory.getLogger(NativePayloadCodec.class);

  private final ObjectMapper mapper;
  private final HelmClient.NativeStringReleaser stringReleaser;

  NativePayloadCodec(ObjectMapper mapper, HelmClient.NativeStringReleaser stringReleaser) {
    this.mapper = mapper;
    this.stringReleaser = stringReleaser;
  }

  String toJson(Object value) {
    try {
      return mapper.writeValueAsString(value);
    } catch (JacksonException e) {
      var type = value == null ? "null" : value.getClass().getSimpleName();
      LOGGER.error("Failed to encode options payload for type '{}'", type, e);
      throw new IllegalArgumentException("Failed to encode options", e);
    }
  }

  NativeShowPayload decodeShowResponse(MemorySegment resultPtr) {
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

  NativeSearchPayload decodeSearchResponse(MemorySegment resultPtr) {
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
}
