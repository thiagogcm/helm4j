package dev.nthings.helm4j.chart;

import org.jspecify.annotations.Nullable;

/** Result of packaging a chart. */
public record PackageChartResult(@Nullable String path) {}
