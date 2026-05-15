package dev.nthings.helm4j.runtime.ffm.internal;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import dev.nthings.helm4j.errors.HelmCommandException;
import dev.nthings.helm4j.errors.HelmFailure;
import dev.nthings.helm4j.errors.HelmRuntimeException;
import dev.nthings.helm4j.release.HookInfo;
import dev.nthings.helm4j.release.Release;
import dev.nthings.helm4j.release.ReleaseStatus;

import org.jspecify.annotations.Nullable;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Shared plumbing for the native gateway implementations: the {@link HelmBridge} handle, the JSON
 * {@link ObjectMapper}, and the encode/decode/error-mapping helpers each domain gateway needs.
 */
final class NativeGatewaySupport {

  private final HelmBridge bridge;
  private final ObjectMapper mapper;

  NativeGatewaySupport(HelmBridge bridge, ObjectMapper mapper) {
    this.bridge = Objects.requireNonNull(bridge, "bridge");
    this.mapper = Objects.requireNonNull(mapper, "mapper");
  }

  /** Invokes the bridge, parses the JSON envelope, and returns it without inspecting for errors. */
  JsonNode invokeRoot(String operation, BridgeCall call) {
    return parse(invoke(call, operation, "invokeNative"), operation);
  }

  /**
   * Invokes the bridge and throws {@link HelmCommandException} if the envelope carries an error.
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
      throw runtimeFailure("decodeResponse", operation, "Failed to decode native response", error);
    }
  }

  byte[] toJsonBytes(Map<String, Object> payload, String operation) {
    if (payload.isEmpty()) {
      return EMPTY_JSON_OBJECT;
    }
    try {
      return mapper.writeValueAsBytes(payload);
    } catch (JacksonException error) {
      throw runtimeFailure("encodeOptions", operation, "Failed to encode native options", error);
    }
  }

  /**
   * Returns {@code value} or throws {@link dev.nthings.helm4j.errors.HelmRuntimeException} with a
   * uniform "Native &lt;operation&gt; response missing &lt;what&gt;" message if it is {@code null}.
   */
  static <T> T requireResponse(@Nullable T value, String operation, String what) {
    if (value == null) {
      throw runtimeFailure(
          "decodeResponse", operation, "Native " + operation + " response missing " + what, null);
    }
    return value;
  }

  private static final byte[] EMPTY_JSON_OBJECT = {'{', '}'};

  private JsonNode parse(byte[] payload, String operation) {
    try {
      return mapper.readTree(payload);
    } catch (JacksonException error) {
      throw runtimeFailure("decodeResponse", operation, "Failed to decode native response", error);
    }
  }

  private byte[] invoke(BridgeCall call, String operation, String stage) {
    final byte @Nullable [] payload;
    try {
      payload = call.invoke(bridge);
    } catch (RuntimeException error) {
      throw runtimeFailure(stage, operation, "Native bridge invocation failed", error);
    }

    if (payload == null || payload.length == 0) {
      throw runtimeFailure(stage, operation, "Native bridge returned empty response", null);
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

  static HelmCommandException asException(OperationError error) {
    return new HelmCommandException(failure(error));
  }

  /** Maps a native operation error to the uniform {@link HelmFailure} carrier. */
  static HelmFailure failure(OperationError error) {
    return new HelmFailure(messageOrUnknown(error.message()), error.stage(), error.operation());
  }

  static HelmRuntimeException runtimeFailure(
      String stage, String operation, String message, @Nullable Throwable cause) {
    return new HelmRuntimeException(
        message + " (operation=" + operation + ", stage=" + stage + ")", cause);
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

  static Release mapReleasePayload(NativeReleasePayload r, String operation) {
    return new Release(
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
      throw runtimeFailure(
          "decodeResponse", operation, "Invalid timestamp for " + field + ": " + value, error);
    }
  }

  static <T> List<T> listOrEmpty(@Nullable List<T> value) {
    return value == null ? List.of() : value;
  }

  /** A single native bridge call; receives the bridge so callers need not hold a reference. */
  @FunctionalInterface
  interface BridgeCall {
    byte @Nullable [] invoke(HelmBridge bridge);
  }

  record OperationError(String message, @Nullable String stage, String operation) {}
}
