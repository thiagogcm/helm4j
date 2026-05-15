package dev.nthings.helm4j.errors;

import org.jspecify.annotations.Nullable;

/**
 * Configuration error caught before any runtime call (missing mandatory inputs, conflicting flags).
 */
public final class HelmConfigurationException extends HelmException {

  public HelmConfigurationException(String message) {
    super(message);
  }

  public HelmConfigurationException(String message, @Nullable Throwable cause) {
    super(message, cause);
  }
}
