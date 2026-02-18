package dev.nthings.helm4j.release;

import java.util.Objects;

/** Rollback failed at domain-operation level. */
public record RollbackFailure(String message, String stage, String operation)
    implements RollbackResult {

  public RollbackFailure {
    message = Objects.requireNonNull(message, "message");
  }
}
