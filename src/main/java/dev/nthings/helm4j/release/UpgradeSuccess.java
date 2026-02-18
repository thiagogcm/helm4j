package dev.nthings.helm4j.release;

import java.util.Objects;

/** Successful upgrade completion. */
public record UpgradeSuccess(ReleaseInfo release) implements UpgradeResult {

  public UpgradeSuccess {
    release = Objects.requireNonNull(release, "release");
  }
}
