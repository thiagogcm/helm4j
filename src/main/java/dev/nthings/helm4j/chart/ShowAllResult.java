package dev.nthings.helm4j.chart;

import java.util.List;

import dev.nthings.helm4j.internal.model.ModelSupport;

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
    customResourceDefinitions = ModelSupport.immutableListOrEmpty(customResourceDefinitions);
  }
}
