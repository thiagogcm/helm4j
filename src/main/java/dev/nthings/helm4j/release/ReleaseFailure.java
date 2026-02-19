package dev.nthings.helm4j.release;

import java.util.Objects;

/** Failed release operation mapped from native domain errors. */
public record ReleaseFailure(String message, String stage, String operation)
    implements InstallResult, UpgradeResult, UninstallResult, RollbackResult {

  public ReleaseFailure {
    message = Objects.requireNonNull(message, "message");
  }
}
