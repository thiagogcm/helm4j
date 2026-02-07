package dev.nthings.helm4j.model;

import java.util.List;
import java.util.Objects;

/** Result for {@code helm show all}. */
public record ChartDetails(
    String chartReference,
    String resolvedChartPath,
    String metadataYaml,
    String valuesYaml,
    String readmeText,
    List<String> customResourceDefinitions,
    String rawOutput) {

  public ChartDetails {
    chartReference = Objects.requireNonNull(chartReference, "chartReference");
    resolvedChartPath = Objects.requireNonNull(resolvedChartPath, "resolvedChartPath");
    metadataYaml = Objects.requireNonNull(metadataYaml, "metadataYaml");
    valuesYaml = Objects.requireNonNull(valuesYaml, "valuesYaml");
    readmeText = Objects.requireNonNull(readmeText, "readmeText");
    customResourceDefinitions =
        List.copyOf(Objects.requireNonNull(customResourceDefinitions, "customResourceDefinitions"));
    rawOutput = Objects.requireNonNull(rawOutput, "rawOutput");
  }
}
