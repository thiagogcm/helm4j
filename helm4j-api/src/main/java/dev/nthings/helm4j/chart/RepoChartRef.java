package dev.nthings.helm4j.chart;

/** Repository chart reference (for example, {@code bitnami/nginx}). */
public record RepoChartRef(String value) implements ChartRef {

  public RepoChartRef {
    value = ChartRef.requireNonBlank(value, "value");
  }

  @Override
  public String asReference() {
    return value;
  }
}
