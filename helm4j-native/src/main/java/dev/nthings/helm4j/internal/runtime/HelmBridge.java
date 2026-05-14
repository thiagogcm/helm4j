package dev.nthings.helm4j.internal.runtime;

import org.jspecify.annotations.Nullable;

/**
 * Transport contract for the native Helm runtime: a thin, byte-array-only bridge over libhelm4j.
 *
 * <p>This is a runtime-internal seam, not a public extension point — it lives in the native module
 * because it carries no domain types and is meaningful only to the FFM implementation. Swapping the
 * transport (e.g. FFM for a process-exec backend) means providing another {@code HelmBridge}.
 *
 * <p>All parameters and return values are raw UTF-8 encoded byte arrays. The bridge implementation
 * is responsible for converting these to the native format required by the underlying library
 * (e.g., null-terminated C strings for FFM). This avoids unnecessary String encoding/decoding at
 * the boundary.
 *
 * <p>Implementations must be safe to call from a single thread. Concurrent access is not required.
 */
public interface HelmBridge {

  byte[] repo(byte @Nullable [] mode, byte @Nullable [] optionsJson);

  byte[] search(byte @Nullable [] mode, byte @Nullable [] optionsJson);

  byte[] show(byte @Nullable [] mode, byte @Nullable [] chartRef, byte @Nullable [] optionsJson);

  byte[] install(
      byte @Nullable [] releaseName, byte @Nullable [] chartRef, byte @Nullable [] optionsJson);

  byte[] upgrade(
      byte @Nullable [] releaseName, byte @Nullable [] chartRef, byte @Nullable [] optionsJson);

  byte[] uninstall(byte @Nullable [] releaseName, byte @Nullable [] optionsJson);

  byte[] status(byte @Nullable [] releaseName, byte @Nullable [] optionsJson);

  byte[] rollback(byte @Nullable [] releaseName, byte @Nullable [] optionsJson);

  byte[] history(byte @Nullable [] releaseName, byte @Nullable [] optionsJson);

  byte[] get(byte @Nullable [] mode, byte @Nullable [] releaseName, byte @Nullable [] optionsJson);

  byte[] list(byte @Nullable [] optionsJson);

  byte[] pull(byte @Nullable [] chartRef, byte @Nullable [] optionsJson);

  byte[] push(byte @Nullable [] chartRef, byte @Nullable [] remote, byte @Nullable [] optionsJson);

  byte[] packageChart(byte @Nullable [] chartPath, byte @Nullable [] optionsJson);

  byte[] dependency(byte @Nullable [] chartPath, byte @Nullable [] optionsJson);

  byte[] registry(
      byte @Nullable [] mode, byte @Nullable [] hostname, byte @Nullable [] optionsJson);

  byte[] test(byte @Nullable [] releaseName, byte @Nullable [] optionsJson);

  byte[] template(
      byte @Nullable [] releaseName, byte @Nullable [] chartRef, byte @Nullable [] optionsJson);

  byte[] lint(byte @Nullable [] chartPath, byte @Nullable [] optionsJson);

  byte[] version();
}
