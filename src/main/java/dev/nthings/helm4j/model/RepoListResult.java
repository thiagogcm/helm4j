package dev.nthings.helm4j.model;

import java.util.List;
import java.util.Objects;

/** Structured response for {@code helm repo list}. */
public record RepoListResult(List<RepoSummary> repositories) {

  public RepoListResult {
    repositories = List.copyOf(Objects.requireNonNull(repositories, "repositories"));
  }

  public int size() {
    return repositories.size();
  }

  public boolean isEmpty() {
    return repositories.isEmpty();
  }
}
