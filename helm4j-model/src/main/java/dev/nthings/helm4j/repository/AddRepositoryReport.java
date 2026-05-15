package dev.nthings.helm4j.repository;

import java.util.Objects;

/** Outcome of a successful {@code repositories().add(...)} call. */
public record AddRepositoryReport(String name, String url) {

  public AddRepositoryReport {
    name = Objects.requireNonNull(name, "name");
    url = Objects.requireNonNull(url, "url");
  }
}
