package dev.nthings.helm4j.internal.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/** Shared value normalization and immutable copy helpers for SDK records. */
public final class ModelSupport {

  private ModelSupport() {}

  public static @Nullable String normalizeBlankToNull(@Nullable String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.strip();
  }

  public static <T> List<T> immutableListOrEmpty(@Nullable List<T> value) {
    if (value == null || value.isEmpty()) {
      return List.of();
    }
    return List.copyOf(value);
  }

  public static <T> Map<String, T> immutableMapOrEmpty(@Nullable Map<String, T> value) {
    if (value == null || value.isEmpty()) {
      return Map.of();
    }
    return Map.copyOf(new LinkedHashMap<>(value));
  }
}
