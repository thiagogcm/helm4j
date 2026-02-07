package dev.nthings.helm4j.model;

import java.util.Objects;

/** Result for {@code helm show readme}. */
public record ChartReadme(
    String chartReference, String resolvedChartPath, String readmeText, String rawOutput) {

  public ChartReadme {
    chartReference = Objects.requireNonNull(chartReference, "chartReference");
    resolvedChartPath = Objects.requireNonNull(resolvedChartPath, "resolvedChartPath");
    readmeText = Objects.requireNonNull(readmeText, "readmeText");
    rawOutput = Objects.requireNonNull(rawOutput, "rawOutput");
  }
}
