package dev.nthings.helm4j.model;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Output modes supported by {@code helm show}. */
public enum ShowMode {
  CHART,
  VALUES,
  README,
  ALL,
  CRDS;

  @JsonCreator
  public static ShowMode fromString(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    return ShowMode.valueOf(raw.toUpperCase(Locale.ROOT));
  }

  @JsonValue
  public String toJson() {
    return name().toLowerCase(Locale.ROOT);
  }
}
