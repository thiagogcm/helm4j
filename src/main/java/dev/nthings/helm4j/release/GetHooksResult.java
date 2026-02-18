package dev.nthings.helm4j.release;

import java.util.List;

/** Result of `helm get hooks`. */
public record GetHooksResult(List<HookInfo> hooks) {

  public GetHooksResult {
    hooks = hooks == null ? List.of() : List.copyOf(hooks);
  }
}
