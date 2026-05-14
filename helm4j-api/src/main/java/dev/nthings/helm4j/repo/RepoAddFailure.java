package dev.nthings.helm4j.repo;

import java.util.Objects;

import dev.nthings.helm4j.errors.HelmFailure;
import dev.nthings.helm4j.errors.HelmFailureCarrier;

/** Failed repository addition outcome, carrying a structured failure. */
public record RepoAddFailure(HelmFailure failure) implements RepoAddResult, HelmFailureCarrier {

  public RepoAddFailure {
    failure = Objects.requireNonNull(failure, "failure");
  }
}
