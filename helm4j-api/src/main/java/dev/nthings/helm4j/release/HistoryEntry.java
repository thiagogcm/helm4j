package dev.nthings.helm4j.release;

import java.time.Instant;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

/** One revision in a release's history. */
public record HistoryEntry(
    int revision,
    @Nullable Instant updated,
    ReleaseStatus status,
    String chart,
    String chartVersion,
    String appVersion,
    @Nullable String description) {

  public HistoryEntry {
    status = Objects.requireNonNullElse(status, ReleaseStatus.UNKNOWN);
  }
}
