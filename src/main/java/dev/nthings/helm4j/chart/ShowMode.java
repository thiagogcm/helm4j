package dev.nthings.helm4j.chart;

/** Supported Helm show operation modes. */
public enum ShowMode {
  CHART("chart"),
  VALUES("values"),
  README("readme"),
  CRDS("crds"),
  ALL("all");

  private final String wireValue;

  ShowMode(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }
}
