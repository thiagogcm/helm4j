package dev.nthings.helm4j.repo;

import java.util.List;
import java.util.Optional;

import dev.nthings.helm4j.internal.model.ModelSupport;

/** Structured response for repository update operations. */
public record RepoUpdateResult(List<RepoUpdateEntry> repositories) {

  public RepoUpdateResult {
    repositories = ModelSupport.immutableListOrEmpty(repositories);
  }

  public int size() {
    return repositories.size();
  }

  public Optional<RepoUpdateEntry> first() {
    return repositories.isEmpty() ? Optional.empty() : Optional.of(repositories.getFirst());
  }
}
