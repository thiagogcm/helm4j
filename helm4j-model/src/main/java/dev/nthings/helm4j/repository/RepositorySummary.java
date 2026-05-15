package dev.nthings.helm4j.repository;

import java.util.Objects;

/** Single repository entry returned by list operations. */
public record RepositorySummary(String name, String url) {

  public RepositorySummary {
    name = Objects.requireNonNull(name, "name");
    url = Objects.requireNonNull(url, "url");
  }
}
