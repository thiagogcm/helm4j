package dev.nthings.helm4j.chart;

/** Result of {@code show readme}. */
public record ShowReadmeResult(
    String chartReference, String resolvedPath, String readmeText, String rawOutput) {}
