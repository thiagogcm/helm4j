package dev.nthings.helm4j.release;

import java.util.List;

import dev.nthings.helm4j.internal.model.ModelSupport;

/** Result of `helm get hooks`. */
public record GetHooksResult(List<HookInfo> hooks) {

  public GetHooksResult {
    hooks = ModelSupport.immutableListOrEmpty(hooks);
  }
}
