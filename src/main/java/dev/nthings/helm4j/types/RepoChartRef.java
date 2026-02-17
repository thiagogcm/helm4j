package dev.nthings.helm4j.types;

import java.util.Objects;

/** Repository chart reference (for example, {@code bitnami/nginx}). */
public record RepoChartRef(String value) implements ChartRef {

  public RepoChartRef {
    value = normalize(value, "value");
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
