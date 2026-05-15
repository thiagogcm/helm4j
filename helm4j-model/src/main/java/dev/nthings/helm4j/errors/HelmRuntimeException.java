package dev.nthings.helm4j.errors;

import org.jspecify.annotations.Nullable;

/**
 * The runtime itself failed: native library not found, FFM allocation failed, protocol parse error.
 */
public final class HelmRuntimeException extends HelmException {

  public HelmRuntimeException(String message) {
    super(message);
  }

  public HelmRuntimeException(String message, @Nullable Throwable cause) {
    super(message, cause);
  }
}
