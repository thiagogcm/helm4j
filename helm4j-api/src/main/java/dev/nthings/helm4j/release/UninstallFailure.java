package dev.nthings.helm4j.release;

import java.util.Objects;

import dev.nthings.helm4j.errors.HelmFailure;
import dev.nthings.helm4j.errors.HelmFailureCarrier;

/** Failed {@code uninstall} operation, carrying a structured failure. */
public record UninstallFailure(HelmFailure failure) implements UninstallResult, HelmFailureCarrier {

  public UninstallFailure {
    failure = Objects.requireNonNull(failure, "failure");
  }
}
