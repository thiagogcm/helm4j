package dev.nthings.helm4j.release;

import java.util.Objects;

/** Upgrade failed at domain-operation level. */
public record UpgradeFailure(String message, String stage, String operation)
    implements UpgradeResult {

  public UpgradeFailure {
    message = Objects.requireNonNull(message, "message");
  }
}
