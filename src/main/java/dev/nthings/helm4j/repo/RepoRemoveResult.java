package dev.nthings.helm4j.repo;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Structured response for repository remove operations. */
public record RepoRemoveResult(List<String> removed) {

  public RepoRemoveResult {
    removed = List.copyOf(Objects.requireNonNullElse(removed, List.of()));
  }

  public int size() {
    return removed.size();
  }

  public Optional<String> first() {
    return removed.stream().findFirst();
  }
}
