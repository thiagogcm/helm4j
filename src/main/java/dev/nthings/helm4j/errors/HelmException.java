package dev.nthings.helm4j.errors;

/** Exception surfaced when the native bridge reports a transport or runtime error. */
public final class HelmException extends RuntimeException {

  private final String stage;
  private final String operation;

  public HelmException(String message, String stage, String operation) {
    super(message);
    this.stage = stage;
    this.operation = operation;
  }

  public HelmException(String message, String stage, String operation, Throwable cause) {
    super(message, cause);
    this.stage = stage;
    this.operation = operation;
  }

  public String stage() {
    return stage;
  }

  public String operation() {
    return operation;
  }
}
