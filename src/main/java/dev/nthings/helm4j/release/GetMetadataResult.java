package dev.nthings.helm4j.release;

import java.time.Instant;
import java.util.Objects;

/** Result of `helm get metadata`. */
public record GetMetadataResult(
    String name,
    String namespace,
    int revision,
    ReleaseStatus status,
    String chart,
    String chartVersion,
    String appVersion,
    Instant deployedAt) {

  public GetMetadataResult {
    status = Objects.requireNonNullElse(status, ReleaseStatus.UNKNOWN);
  }
}
