package dev.nthings.helm4j.release;

import java.util.Objects;

/** Successful install or upgrade completion. */
public record ReleaseSuccess(ReleaseInfo release) implements ReleaseOutcome {

  public ReleaseSuccess {
    release = Objects.requireNonNull(release, "release");
  }
}
