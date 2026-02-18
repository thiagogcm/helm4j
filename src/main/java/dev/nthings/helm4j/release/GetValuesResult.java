package dev.nthings.helm4j.release;

import java.util.Map;

import dev.nthings.helm4j.internal.model.ModelSupport;

/** Result of `helm get values`. */
public record GetValuesResult(Map<String, Object> values) {

  public GetValuesResult {
    values = ModelSupport.immutableMapOrEmpty(values);
  }
}
