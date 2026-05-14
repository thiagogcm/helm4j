package dev.nthings.helm4j.release;

import java.util.List;
import java.util.Map;

import dev.nthings.helm4j.internal.model.ModelSupport;

/** Result of `helm get all`. */
public record GetAllResult(
    ReleaseInfo release,
    Map<String, Object> values,
    String manifest,
    List<HookInfo> hooks,
    String notes) {

  public GetAllResult {
    values = ModelSupport.immutableMapOrEmpty(values);
    hooks = ModelSupport.immutableListOrEmpty(hooks);
  }
}
