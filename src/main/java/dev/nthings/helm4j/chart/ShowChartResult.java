package dev.nthings.helm4j.chart;

/** Result of {@code show chart}. */
public record ShowChartResult(
    String chartReference, String resolvedPath, String metadataYaml, String rawOutput) {}
