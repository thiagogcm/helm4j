package dev.nthings.helm4j.release;

import java.util.Objects;

/** Upgrade accepted but pending completion on cluster state. */
public record UpgradePending(ReleaseInfo release) implements UpgradeResult {

  public UpgradePending {
    release = Objects.requireNonNull(release, "release");
  }
}
