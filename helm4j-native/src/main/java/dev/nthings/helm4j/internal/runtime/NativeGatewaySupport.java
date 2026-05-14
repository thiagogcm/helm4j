package dev.nthings.helm4j.internal.runtime;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import dev.nthings.helm4j.errors.HelmException;
import dev.nthings.helm4j.errors.HelmFailure;
import dev.nthings.helm4j.release.HookInfo;
import dev.nthings.helm4j.release.ReleaseInfo;
import dev.nthings.helm4j.release.ReleaseStatus;

import org.jspecify.annotations.Nullable;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Shared plumbing for the native gateway implementations: the
 * {@link HelmBridge} handle, the JSON
 * {@link ObjectMapper}, and the encode/decode/error-mapping helpers each domain
 * gateway needs.
 *
 * <p>
 * One instance is created per {@link NativeStructGateway} and handed to every
 * sub-gateway, so
 * the bridge and mapper are configured once and reused.
 */
final class NativeGatewaySupport {

  private final HelmBridge bridge;
  private final ObjectMapper mapper;

  NativeGatewaySupport(HelmBridge bridge, ObjectMapper mapper) {
    this.bridge = Objects.requireNonNull(bridge, "bridge");
    this.mapper = Objects.requireNonNull(mapper, "mapper");
  }

  /**
   * Invokes the bridge, parses the JSON envelope, and returns it without
   * inspecting for errors.
   */
  JsonNode invokeRoot(String operation, BridgeCall call) {
    return parse(invoke(call, operation, "invokeNative"), operation);
  }

  /**
   * Invokes the bridge and throws {@link HelmException} if the envelope carries
   * an error.
   */
  JsonNode invokeRootOrThrow(String operation, BridgeCall call) {
    var root = invokeRoot(operation, call);
    var failure = operationError(root, operation);
    if (failure != null) {
      throw asException(failure);
    }
    return root;
  }

  <T> T convert(JsonNode node, Class<T> type, String operation) {
    try {
      return mapper.treeToValue(node, type);
    } catch (JacksonException error) {
      throw new HelmException(
          "Failed to decode native response", "decodeResponse", operation, error);
    }
  }

  byte[] toJsonBytes(Map<String, Object> payload, String operation) {
    try {
      return mapper.writeValueAsBytes(payload);
    } catch (JacksonException error) {
      throw new HelmException("Failed to encode native options", "encodeOptions", operation, error);
    }
  }

  private JsonNode parse(byte[] payload, String operation) {
    try {
      return mapper.readTree(payload);
    } catch (JacksonException error) {
      throw new HelmException(
          "Failed to decode native response", "decodeResponse", operation, error);
    }
  }

  private byte[] invoke(BridgeCall call, String operation, String stage) {
    final byte @Nullable [] payload;
    try {
      payload = call.invoke(bridge);
    } catch (RuntimeException error) {
      throw new HelmException("Native bridge invocation failed", stage, operation, error);
    }

    if (payload == null || payload.length == 0) {
      throw new HelmException("Native bridge returned empty response", stage, operation);
    }
    return payload;
  }

  static @Nullable OperationError operationError(
      @Nullable JsonNode node, String fallbackOperation) {
    if (node == null || !node.has("error")) {
      return null;
    }

    var message = text(node, "error");
    if (message == null || message.isBlank()) {
      return null;
    }

    return new OperationError(
        message,
        text(node, "stage"),
        fallbackOperation(text(node, "operation"), fallbackOperation));
  }

  static HelmException asException(OperationError error) {
    return new HelmException(failure(error));
  }

  /** Maps a native operation error to the uniform {@link HelmFailure} carrier. */
  static HelmFailure failure(OperationError error) {
    return new HelmFailure(messageOrUnknown(error.message()), error.stage(), error.operation());
  }

  private static @Nullable String text(JsonNode node, String field) {
    var value = node.get(field);
    if (value == null || value.isNull()) {
      return null;
    }
    return value.asString();
  }

  static byte @Nullable [] utf8(@Nullable String value) {
    if (value == null) {
      return null;
    }
    return value.getBytes(StandardCharsets.UTF_8);
  }

  static ReleaseInfo mapReleasePayload(NativeReleasePayload r, String operation) {
    return new ReleaseInfo(
        r.name(),
        r.namespace(),
        r.revision(),
        ReleaseStatus.fromWireValue(r.status()),
        r.description(),
        parseTimestamp(r.firstDeployed(), operation, "firstDeployed"),
        parseTimestamp(r.lastDeployed(), operation, "lastDeployed"),
        r.chartName(),
        r.chartVersion(),
        r.appVersion(),
        r.notes());
  }

  static List<HookInfo> mapHooks(@Nullable List<HookPayload> hooks) {
    return listOrEmpty(hooks).stream()
        .map(h -> new HookInfo(h.name(), h.kind(), h.path(), listOrEmpty(h.events()), h.weight()))
        .toList();
  }

  static Map<String, Object> mapOrEmpty(@Nullable Map<String, Object> value) {
    return value == null ? Map.of() : value;
  }

  private static String fallbackOperation(@Nullable String operation, String fallback) {
    return operation == null || operation.isBlank() ? fallback : operation;
  }

  static String messageOrUnknown(@Nullable String message) {
    if (message == null || message.isBlank()) {
      return "Unknown native operation error";
    }
    return message;
  }

  static @Nullable Instant parseTimestamp(@Nullable String value, String operation, String field) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Instant.parse(value);
    } catch (DateTimeParseException error) {
      throw new HelmException(
          "Invalid timestamp for " + field + ": " + value, "decodeResponse", operation, error);
    }
  }

  static <T> List<T> listOrEmpty(@Nullable List<T> value) {
    return value == null ? List.of() : value;
  }

  /**
   * A single native bridge call; receives the bridge so callers need not hold a
   * reference.
   */
  @FunctionalInterface
  interface BridgeCall {
    byte @Nullable [] invoke(HelmBridge bridge);
  }

  record OperationError(String message, @Nullable String stage, String operation) {
  }
}
