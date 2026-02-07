package dev.nthings.helm4j.model;

import java.util.Objects;

/** Result for {@code helm show chart}. */
public record ChartMetadata(
    String chartReference, String resolvedChartPath, String metadataYaml, String rawOutput) {

  public ChartMetadata {
    chartReference = Objects.requireNonNull(chartReference, "chartReference");
    resolvedChartPath = Objects.requireNonNull(resolvedChartPath, "resolvedChartPath");
    metadataYaml = Objects.requireNonNull(metadataYaml, "metadataYaml");
    rawOutput = Objects.requireNonNull(rawOutput, "rawOutput");
  }
}
