package dev.nthings.helm4j.repo;

import java.util.Objects;

/** Single repository entry returned by list operations. */
public record RepoSummary(String name, String url) {

  public RepoSummary {
    name = Objects.requireNonNull(name, "name");
    url = Objects.requireNonNull(url, "url");
  }
}
