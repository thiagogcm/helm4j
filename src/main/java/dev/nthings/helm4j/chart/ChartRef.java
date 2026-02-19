package dev.nthings.helm4j.chart;

import java.nio.file.Path;
import java.util.Objects;

/** Typed chart reference for repository, OCI, and local chart sources. */
public sealed interface ChartRef permits RepoChartRef, OciChartRef, LocalChartRef {

  static RepoChartRef repo(String value) {
    return new RepoChartRef(value);
  }

  static OciChartRef oci(String value) {
    return new OciChartRef(value);
  }

  static LocalChartRef local(Path value) {
    return new LocalChartRef(value);
  }

  static String requireNonBlank(String value, String field) {
    Objects.requireNonNull(value, field);
    var normalized = value.trim();
    if (normalized.isEmpty()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return normalized;
  }

  String asReference();
}
