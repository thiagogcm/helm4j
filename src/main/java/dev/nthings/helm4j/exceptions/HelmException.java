package dev.nthings.helm4j.exceptions;

/** Exception surfaced when the native Helm bindings report an error payload. */
public final class HelmException extends RuntimeException {
  private final String stage;
  private final String mode;
  private final String chartRef;
  private final String chartPath;

  public HelmException(
      String message, String stage, String mode, String chartRef, String chartPath) {
    super(message);
    this.stage = stage;
    this.mode = mode;
    this.chartRef = chartRef;
    this.chartPath = chartPath;
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
}
