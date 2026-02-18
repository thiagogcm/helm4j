package dev.nthings.helm4j.release;

import java.util.Objects;

/** Result of a release status query. */
public record StatusResult(ReleaseInfo release) {

  public StatusResult {
    release = Objects.requireNonNull(release, "release");
  }
}
