package dev.nthings.helm4j.release;

/** Mode for `helm get` subcommands. */
public enum GetMode {
  ALL("all"),
  VALUES("values"),
  MANIFEST("manifest"),
  HOOKS("hooks"),
  NOTES("notes"),
  METADATA("metadata");

  private final String wireValue;

  GetMode(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }
}
