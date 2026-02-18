package dev.nthings.helm4j.chart;

import java.util.List;

import dev.nthings.helm4j.internal.model.ModelSupport;

/** Result of {@code show crds}. */
public record ShowCrdsResult(
    String chartReference,
    String resolvedPath,
    List<String> customResourceDefinitions,
    String rawOutput) {

  public ShowCrdsResult {
    customResourceDefinitions = ModelSupport.immutableListOrEmpty(customResourceDefinitions);
  }
}
