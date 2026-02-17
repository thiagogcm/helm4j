package dev.nthings.helm4j.types;

import java.util.Objects;

/** OCI chart reference (for example, {@code oci://registry-1.docker.io/bitnamicharts/nginx}). */
public record OciChartRef(String value) implements ChartRef {

  public OciChartRef {
    value = normalize(value, "value");
    if (!value.startsWith("oci://")) {
      throw new IllegalArgumentException("OCI chart reference must start with 'oci://'");
    }
  }

  @Override
  public String asReference() {
    return value;
  }

  private static String normalize(String value, String field) {
    Objects.requireNonNull(value, field);
    var normalized = value.trim();
    if (normalized.isEmpty()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return normalized;
  }
}
