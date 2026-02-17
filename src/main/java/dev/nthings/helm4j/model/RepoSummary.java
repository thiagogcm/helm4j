package dev.nthings.helm4j.model;

import java.util.Objects;

/** A single repository entry returned by {@code helm repo list}. */
public record RepoSummary(String name, String url) {

  public RepoSummary {
    name = Objects.requireNonNull(name, "name");
    url = Objects.requireNonNull(url, "url");
  }
}
