package dev.nthings.helm4j.release;

import java.util.List;

import dev.nthings.helm4j.internal.model.ModelSupport;

/** Information about a Helm hook on a release. */
public record HookInfo(String name, String kind, String path, List<String> events, int weight) {

  public HookInfo {
    events = ModelSupport.immutableListOrEmpty(events);
  }
}
