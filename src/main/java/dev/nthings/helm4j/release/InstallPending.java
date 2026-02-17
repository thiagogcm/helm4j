package dev.nthings.helm4j.release;

import java.util.Objects;

/** Install accepted but pending completion on cluster state. */
public record InstallPending(ReleaseInfo release) implements InstallResult {

  public InstallPending {
    release = Objects.requireNonNull(release, "release");
  }
}
