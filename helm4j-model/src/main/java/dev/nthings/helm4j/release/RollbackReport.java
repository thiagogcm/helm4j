package dev.nthings.helm4j.release;

import java.util.Objects;

/** Outcome of a successful rollback. */
public record RollbackReport(String releaseName, int revision) {

  public RollbackReport {
    releaseName = Objects.requireNonNull(releaseName, "releaseName");
  }
}
