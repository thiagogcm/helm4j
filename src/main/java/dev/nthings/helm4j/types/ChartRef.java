package dev.nthings.helm4j.types;

import java.nio.file.Path;

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

  String asReference();
}
