package dev.nthings.helm4j.chart;

import java.nio.file.Path;

import org.jspecify.annotations.Nullable;

/**
 * Typed chart reference for repository, OCI, and local chart sources.
 *
 * <p>A {@code ChartRef} identifies <em>which</em> chart to use and, for remote sources, <em>which
 * version</em>. How to fetch it — credentials, TLS, an explicit repository URL — is a separate
 * {@link ChartSource} concern.
 */
public sealed interface ChartRef permits RepoChartRef, OciChartRef, LocalChartRef {

  /** A repository chart reference such as {@code bitnami/nginx}, at the latest version. */
  static RepoChartRef repo(String value) {
    return new RepoChartRef(value, null);
  }

  /** A repository chart reference pinned to a specific version. */
  static RepoChartRef repo(String value, String version) {
    return new RepoChartRef(value, version);
  }

  /** An OCI chart reference such as {@code oci://registry/chart}, at the latest version. */
  static OciChartRef oci(String value) {
    return new OciChartRef(value, null);
  }

  /** An OCI chart reference pinned to a specific version. */
  static OciChartRef oci(String value, String version) {
    return new OciChartRef(value, version);
  }

  /** A local filesystem chart reference (directory or packaged archive). */
  static LocalChartRef local(Path value) {
    return new LocalChartRef(value);
  }

  /**
   * The wire reference passed to Helm (a repo path, an {@code oci://} URI, or a filesystem path).
   */
  String asReference();

  /** The requested chart version, or {@code null} to resolve the latest available version. */
  default @Nullable String version() {
    return null;
  }
}
