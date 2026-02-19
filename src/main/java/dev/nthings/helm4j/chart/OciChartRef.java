package dev.nthings.helm4j.chart;

/** OCI chart reference (for example, {@code oci://registry-1.docker.io/bitnamicharts/nginx}). */
public record OciChartRef(String value) implements ChartRef {

  public OciChartRef {
    value = ChartRef.requireNonBlank(value, "value");
    if (!value.startsWith("oci://")) {
      throw new IllegalArgumentException("OCI chart reference must start with 'oci://'");
    }
  }

  @Override
  public String asReference() {
    return value;
  }
}
