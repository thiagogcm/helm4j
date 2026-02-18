package dev.nthings.helm4j.release;

import java.util.Map;

/** Result of `helm get values`. */
public record GetValuesResult(Map<String, Object> values) {

  public GetValuesResult {
    values = values == null ? Map.of() : Map.copyOf(values);
  }
}
