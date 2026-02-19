package dev.nthings.helm4j.release;

import java.util.Objects;

/** Install or upgrade accepted but pending completion on cluster state. */
public record ReleasePending(ReleaseInfo release) implements InstallResult, UpgradeResult {

  public ReleasePending {
    release = Objects.requireNonNull(release, "release");
  }
}
