package dev.nthings.helm4j.model;

import java.util.Objects;

/** A single chart entry returned by {@code helm search repo}. */
public record ChartSummary(
    String name, String version, String appVersion, String description, int score) {

  public ChartSummary {
    name = Objects.requireNonNull(name, "name");
    version = Objects.requireNonNull(version, "version");
    appVersion = Objects.requireNonNull(appVersion, "appVersion");
    description = Objects.requireNonNull(description, "description");
  }
}
