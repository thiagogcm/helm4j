package dev.nthings.helm4j.release;

import java.util.Locale;

/** Typed Helm release status normalized from native wire values. */
public enum ReleaseStatus {
  UNKNOWN("unknown"),
  DEPLOYED("deployed"),
  UNINSTALLED("uninstalled"),
  SUPERSEDED("superseded"),
  FAILED("failed"),
  UNINSTALLING("uninstalling"),
  PENDING("pending"),
  PENDING_INSTALL("pending-install"),
  PENDING_UPGRADE("pending-upgrade"),
  PENDING_ROLLBACK("pending-rollback"),
  PENDING_UNINSTALL("pending-uninstall"),
  PENDING_TEST("pending-test");

  private final String wireValue;

  ReleaseStatus(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }

  public boolean isPending() {
    return switch (this) {
      case PENDING,
          PENDING_INSTALL,
          PENDING_UPGRADE,
          PENDING_ROLLBACK,
          PENDING_UNINSTALL,
          PENDING_TEST ->
          true;
      default -> false;
    };
  }

  public static ReleaseStatus fromWireValue(String value) {
    if (value == null || value.isBlank()) {
      return UNKNOWN;
    }

    var normalized = value.strip().toLowerCase(Locale.ROOT).replace('_', '-');
    return switch (normalized) {
      case "deployed" -> DEPLOYED;
      case "uninstalled" -> UNINSTALLED;
      case "superseded" -> SUPERSEDED;
      case "failed" -> FAILED;
      case "uninstalling" -> UNINSTALLING;
      case "pending-install" -> PENDING_INSTALL;
      case "pending-upgrade" -> PENDING_UPGRADE;
      case "pending-rollback" -> PENDING_ROLLBACK;
      case "pending-uninstall" -> PENDING_UNINSTALL;
      case "pending-test" -> PENDING_TEST;
      default -> normalized.startsWith("pending") ? PENDING : UNKNOWN;
    };
  }
}
