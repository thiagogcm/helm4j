package dev.nthings.helm4j.release;

/** Dry-run strategy supported by Helm v4 actions. */
public enum DryRunMode {
  NONE("none"),
  CLIENT("client"),
  SERVER("server");

  private final String wireValue;

  DryRunMode(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }
}
