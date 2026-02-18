package dev.nthings.helm4j.release;

import java.util.Objects;

/** Uninstall failed at domain-operation level. */
public record UninstallFailure(String message, String stage, String operation)
    implements UninstallResult {

  public UninstallFailure {
    message = Objects.requireNonNull(message, "message");
  }
}
