package dev.nthings.helm4j.repo;

import java.util.List;
import java.util.Optional;

import dev.nthings.helm4j.internal.model.ModelSupport;

/** Structured response for repository list operations. */
public record RepoListResult(List<RepoSummary> repositories) {

  public RepoListResult {
    repositories = ModelSupport.immutableListOrEmpty(repositories);
  }

  public int size() {
    return repositories.size();
  }

  public Optional<RepoSummary> first() {
    return repositories.isEmpty() ? Optional.empty() : Optional.of(repositories.getFirst());
  }
}
