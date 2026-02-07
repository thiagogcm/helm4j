package dev.nthings.helm4j.model;

import java.util.Objects;

/** Result for {@code helm show values}. */
public record ChartValues(
    String chartReference, String resolvedChartPath, String valuesYaml, String rawOutput) {

  public ChartValues {
    chartReference = Objects.requireNonNull(chartReference, "chartReference");
    resolvedChartPath = Objects.requireNonNull(resolvedChartPath, "resolvedChartPath");
    valuesYaml = Objects.requireNonNull(valuesYaml, "valuesYaml");
    rawOutput = Objects.requireNonNull(rawOutput, "rawOutput");
  }
}
