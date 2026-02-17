package dev.nthings.helm4j.repo;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Structured response for repository list operations. */
public record RepoListResult(List<RepoSummary> repositories) {

  public RepoListResult {
    repositories = List.copyOf(Objects.requireNonNullElse(repositories, List.of()));
  }

  public int size() {
    return repositories.size();
  }

  public Optional<RepoSummary> first() {
    return repositories.stream().findFirst();
  }
}
