package dev.nthings.helm4j.repo;

import java.util.Objects;

/** Successful repository addition outcome. */
public record RepoAddSuccess(String name, String url) implements RepoAddResult {

  public RepoAddSuccess {
    name = Objects.requireNonNull(name, "name");
    url = Objects.requireNonNull(url, "url");
  }
}
