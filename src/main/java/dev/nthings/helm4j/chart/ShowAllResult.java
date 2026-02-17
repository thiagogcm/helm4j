package dev.nthings.helm4j.chart;

import java.util.List;
import java.util.Objects;

/** Result of {@code show all}. */
public record ShowAllResult(
    String chartReference,
    String resolvedPath,
    String metadataYaml,
    String valuesYaml,
    String readmeText,
    List<String> customResourceDefinitions,
    String rawOutput) {

  public ShowAllResult {
    customResourceDefinitions =
        List.copyOf(Objects.requireNonNullElse(customResourceDefinitions, List.of()));
  }
}
