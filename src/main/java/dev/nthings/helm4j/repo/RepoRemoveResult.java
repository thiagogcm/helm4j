package dev.nthings.helm4j.repo;

import java.util.List;
import java.util.Optional;

import dev.nthings.helm4j.internal.model.ModelSupport;

/** Structured response for repository remove operations. */
public record RepoRemoveResult(List<String> removed) {

  public RepoRemoveResult {
    removed = ModelSupport.immutableListOrEmpty(removed);
  }

  public int size() {
    return removed.size();
  }

  public Optional<String> first() {
    return removed.isEmpty() ? Optional.empty() : Optional.of(removed.getFirst());
  }
}
