package dev.nthings.helm4j.release;

import java.util.Objects;

import dev.nthings.helm4j.errors.HelmFailure;
import dev.nthings.helm4j.errors.HelmFailureCarrier;

/** Failed {@code rollback} operation, carrying a structured failure. */
public record RollbackFailure(HelmFailure failure) implements RollbackResult, HelmFailureCarrier {

  public RollbackFailure {
    failure = Objects.requireNonNull(failure, "failure");
  }
}
