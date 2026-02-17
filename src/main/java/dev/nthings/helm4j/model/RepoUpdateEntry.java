package dev.nthings.helm4j.model;

import java.util.Objects;

/** Per-repository status entry returned by {@code helm repo update}. */
public record RepoUpdateEntry(String name, String status) {

  public RepoUpdateEntry {
    name = Objects.requireNonNull(name, "name");
    status = Objects.requireNonNull(status, "status");
  }
}
