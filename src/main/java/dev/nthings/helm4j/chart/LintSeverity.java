package dev.nthings.helm4j.chart;

/** Severity level for lint messages. */
public enum LintSeverity {
  UNKNOWN,
  INFO,
  WARNING,
  ERROR;

  /** Parse severity from the wire string value returned by the native bridge. */
  public static LintSeverity fromWireValue(String value) {
    if (value == null) return UNKNOWN;
    return switch (value.toUpperCase()) {
      case "INFO" -> INFO;
      case "WARNING" -> WARNING;
      case "ERROR" -> ERROR;
      default -> UNKNOWN;
    };
  }
}
