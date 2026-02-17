package dev.nthings.helm4j.release;

/** Wait strategy supported by Helm v4 install/upgrade actions. */
public enum WaitMode {
  WATCHER("watcher"),
  HOOK_ONLY("hookOnly"),
  LEGACY("legacy");

  private final String wireValue;

  WaitMode(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }
}
