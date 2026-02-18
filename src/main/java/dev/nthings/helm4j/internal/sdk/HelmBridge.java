package dev.nthings.helm4j.internal.sdk;

/**
 * Service Provider Interface for native Helm operations.
 *
 * <p>
 * All parameters and return values are raw UTF-8 encoded byte arrays. The bridge implementation
 * is responsible for converting these to the native format required by the underlying library
 * (e.g., null-terminated C strings for FFM). This avoids unnecessary String encoding/decoding at the boundary.
 *
 * <p>
 * Implementations must be safe to call from a single thread. Concurrent access is not required.
 */
public interface HelmBridge {

  byte[] repo(byte[] mode, byte[] optionsJson);

  byte[] search(byte[] mode, byte[] optionsJson);

  byte[] show(byte[] mode, byte[] chartRef, byte[] optionsJson);

  byte[] install(byte[] releaseName, byte[] chartRef, byte[] optionsJson);
}
