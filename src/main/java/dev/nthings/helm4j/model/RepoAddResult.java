package dev.nthings.helm4j.model;

import java.util.Objects;

/** Structured response for {@code helm repo add}. */
public record RepoAddResult(String name, String url) {

  public RepoAddResult {
    name = Objects.requireNonNull(name, "name");
    url = Objects.requireNonNull(url, "url");
  }
}
