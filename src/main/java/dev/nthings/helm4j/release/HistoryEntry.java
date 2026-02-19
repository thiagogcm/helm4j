package dev.nthings.helm4j.release;

import java.time.Instant;
import java.util.Objects;

/** One revision in a release's history. */
public record HistoryEntry(
    int revision,
    Instant updated,
    ReleaseStatus status,
    String chart,
    String chartVersion,
    String appVersion,
    String description) {

  public HistoryEntry {
    status = Objects.requireNonNullElse(status, ReleaseStatus.UNKNOWN);
  }
}
