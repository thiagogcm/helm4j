package dev.nthings.helm4j.release;

import java.time.Instant;
import java.util.Objects;

/** Core Helm release metadata returned by install operations. */
public record ReleaseInfo(
    String name,
    String namespace,
    int revision,
    ReleaseStatus status,
    String description,
    Instant firstDeployed,
    Instant lastDeployed,
    String chartName,
    String chartVersion,
    String appVersion,
    String notes) {

  public ReleaseInfo {
    status = Objects.requireNonNullElse(status, ReleaseStatus.UNKNOWN);
  }
}
