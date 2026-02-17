package dev.nthings.helm4j.model;

import java.util.List;
import java.util.Objects;

/** Structured response for {@code helm repo remove}. */
public record RepoRemoveResult(List<String> removed) {

  public RepoRemoveResult {
    removed = List.copyOf(Objects.requireNonNull(removed, "removed"));
  }

  public int size() {
    return removed.size();
  }

  public boolean isEmpty() {
    return removed.isEmpty();
  }
}
