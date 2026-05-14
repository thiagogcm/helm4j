package dev.nthings.helm4j.chart;

import dev.nthings.helm4j.internal.model.ModelSupport;

import org.jspecify.annotations.Nullable;

/**
 * OCI chart reference (for example, {@code oci://registry-1.docker.io/bitnamicharts/nginx}),
 * optionally pinned to a version.
 *
 * @param value the {@code oci://} chart URI
 * @param version the requested version, or {@code null} for the latest available
 */
public record OciChartRef(String value, @Nullable String version) implements ChartRef {

  public OciChartRef {
    value = ChartRef.requireNonBlank(value, "value");
    if (!value.startsWith("oci://")) {
      throw new IllegalArgumentException("OCI chart reference must start with 'oci://'");
    }
    version = ModelSupport.normalizeBlankToNull(version);
  }

  @Override
  public String asReference() {
    return value;
  }
}
