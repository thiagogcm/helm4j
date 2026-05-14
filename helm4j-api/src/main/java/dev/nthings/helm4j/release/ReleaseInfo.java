package dev.nthings.helm4j.release;

import java.time.Instant;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

/** Core Helm release metadata returned by install operations. */
public record ReleaseInfo(
    String name,
    String namespace,
    int revision,
    ReleaseStatus status,
    @Nullable String description,
    @Nullable Instant firstDeployed,
    @Nullable Instant lastDeployed,
    String chartName,
    String chartVersion,
    String appVersion,
    @Nullable String notes) {

  public ReleaseInfo {
    status = Objects.requireNonNullElse(status, ReleaseStatus.UNKNOWN);
  }
}
