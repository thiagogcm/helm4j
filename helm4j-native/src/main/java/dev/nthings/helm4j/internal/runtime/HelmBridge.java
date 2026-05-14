package dev.nthings.helm4j.internal.runtime;

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

  byte[] repo(byte[] mode, byte[] optionsJson);

  byte[] search(byte[] mode, byte[] optionsJson);

  byte[] show(byte[] mode, byte[] chartRef, byte[] optionsJson);

  byte[] install(byte[] releaseName, byte[] chartRef, byte[] optionsJson);

  byte[] upgrade(byte[] releaseName, byte[] chartRef, byte[] optionsJson);

  byte[] uninstall(byte[] releaseName, byte[] optionsJson);

  byte[] status(byte[] releaseName, byte[] optionsJson);

  byte[] rollback(byte[] releaseName, byte[] optionsJson);

  byte[] history(byte[] releaseName, byte[] optionsJson);

  byte[] get(byte[] mode, byte[] releaseName, byte[] optionsJson);

  byte[] list(byte[] optionsJson);

  byte[] pull(byte[] chartRef, byte[] optionsJson);

  byte[] push(byte[] chartRef, byte[] remote, byte[] optionsJson);

  byte[] packageChart(byte[] chartPath, byte[] optionsJson);

  byte[] dependency(byte[] chartPath, byte[] optionsJson);

  byte[] registry(byte[] mode, byte[] hostname, byte[] optionsJson);

  byte[] test(byte[] releaseName, byte[] optionsJson);

  byte[] template(byte[] releaseName, byte[] chartRef, byte[] optionsJson);

  byte[] lint(byte[] chartPath, byte[] optionsJson);

  byte[] version();
}
