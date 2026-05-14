package dev.nthings.helm4j.chart;

import dev.nthings.helm4j.internal.model.ModelSupport;

import org.jspecify.annotations.Nullable;

/**
 * Repository chart reference (for example, {@code bitnami/nginx}), optionally pinned to a version.
 *
 * @param value the {@code repo/chart} reference
 * @param version the requested version, or {@code null} for the latest available
 */
public record RepoChartRef(String value, @Nullable String version) implements ChartRef {

  public RepoChartRef {
    value = ChartRef.requireNonBlank(value, "value");
    version = ModelSupport.normalizeBlankToNull(version);
  }

  @Override
  public String asReference() {
    return value;
  }
}
