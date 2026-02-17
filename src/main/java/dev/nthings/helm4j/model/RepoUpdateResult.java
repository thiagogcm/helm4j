package dev.nthings.helm4j.model;

import java.util.List;
import java.util.Objects;

/** Structured response for {@code helm repo update}. */
public record RepoUpdateResult(List<RepoUpdateEntry> repositories) {

  public RepoUpdateResult {
    repositories = List.copyOf(Objects.requireNonNull(repositories, "repositories"));
  }

  public int size() {
    return repositories.size();
  }

  public boolean isEmpty() {
    return repositories.isEmpty();
  }
}
