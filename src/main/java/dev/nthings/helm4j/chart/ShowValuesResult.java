package dev.nthings.helm4j.chart;

/** Result of {@code show values}. */
public record ShowValuesResult(
    String chartReference, String resolvedPath, String valuesYaml, String rawOutput) {}
