package dev.nthings.helm4j.model;

import java.util.List;
import java.util.Objects;

/** Result for {@code helm show crds}. */
public record ChartCrds(
    String chartReference,
    String resolvedChartPath,
    List<String> customResourceDefinitions,
    String rawOutput) {

  public ChartCrds {
    chartReference = Objects.requireNonNull(chartReference, "chartReference");
    resolvedChartPath = Objects.requireNonNull(resolvedChartPath, "resolvedChartPath");
    customResourceDefinitions =
        List.copyOf(Objects.requireNonNull(customResourceDefinitions, "customResourceDefinitions"));
    rawOutput = Objects.requireNonNull(rawOutput, "rawOutput");
  }
}
