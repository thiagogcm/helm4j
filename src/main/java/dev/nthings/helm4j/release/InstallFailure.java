package dev.nthings.helm4j.release;

import java.util.Objects;

/** Install failed at domain-operation level. */
public record InstallFailure(String message, String stage, String operation)
    implements InstallResult {

  public InstallFailure {
    message = Objects.requireNonNull(message, "message");
  }
}
