package dev.nthings.helm4j.repo;

import java.util.Objects;

/** Failed repository addition outcome mapped from native domain errors. */
public record RepoAddFailure(String message, String stage, String operation)
    implements RepoAddResult {

  public RepoAddFailure {
    message = Objects.requireNonNull(message, "message");
  }
}
