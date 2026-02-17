package dev.nthings.helm4j.release;

import java.util.Objects;

/** Successful install completion. */
public record InstallSuccess(ReleaseInfo release) implements InstallResult {

  public InstallSuccess {
    release = Objects.requireNonNull(release, "release");
  }
}
