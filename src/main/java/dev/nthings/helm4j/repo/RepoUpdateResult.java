package dev.nthings.helm4j.repo;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Structured response for repository update operations. */
public record RepoUpdateResult(List<RepoUpdateEntry> repositories) {

  public RepoUpdateResult {
    repositories = List.copyOf(Objects.requireNonNullElse(repositories, List.of()));
  }

  public int size() {
    return repositories.size();
  }

  public Optional<RepoUpdateEntry> first() {
    return repositories.stream().findFirst();
  }
}
