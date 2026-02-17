package dev.nthings.helm4j.chart;

import java.util.List;
import java.util.Objects;

/** Result of {@code show crds}. */
public record ShowCrdsResult(
    String chartReference,
    String resolvedPath,
    List<String> customResourceDefinitions,
    String rawOutput) {

  public ShowCrdsResult {
    customResourceDefinitions =
        List.copyOf(Objects.requireNonNullElse(customResourceDefinitions, List.of()));
  }
}
