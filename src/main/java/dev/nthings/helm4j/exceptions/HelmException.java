package dev.nthings.helm4j.exceptions;

/** Exception surfaced when the native Helm bindings report an error payload. */
public final class HelmException extends RuntimeException {
  private final String stage;
  private final String mode;
  private final String chartRef;
  private final String chartPath;
  private final String operation;

  public HelmException(
      String message, String stage, String mode, String chartRef, String chartPath) {
    this(message, stage, mode, chartRef, chartPath, null);
  }

  public HelmException(
      String message,
      String stage,
      String mode,
      String chartRef,
      String chartPath,
      String operation) {
    super(message);
    this.stage = stage;
    this.mode = mode;
    this.chartRef = chartRef;
    this.chartPath = chartPath;
    this.operation = operation;
  }

  public String stage() {
    return stage;
  }

  public String mode() {
    return mode;
  }

  public String chartRef() {
    return chartRef;
  }

  public String chartPath() {
    return chartPath;
  }

  public String operation() {
    return operation;
  }
}
