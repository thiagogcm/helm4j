package dev.nthings.helm4j.chart;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Request parameters for linting a chart. */
public record LintRequest(
    Path chartPath,
    boolean strict,
    boolean quiet,
    boolean withSubcharts,
    Map<String, Object> values) {

  public LintRequest {
    chartPath = Objects.requireNonNull(chartPath, "chartPath").toAbsolutePath();
    values = copyMap(values);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private Path chartPath;
    private boolean strict;
    private boolean quiet;
    private boolean withSubcharts;
    private Map<String, Object> values;

    private Builder() {
    }

    public Builder chartPath(Path value) {
      this.chartPath = value;
      return this;
    }

    public Builder strict(boolean value) {
      this.strict = value;
      return this;
    }

    public Builder quiet(boolean value) {
      this.quiet = value;
      return this;
    }

    public Builder withSubcharts(boolean value) {
      this.withSubcharts = value;
      return this;
    }

    public Builder values(Map<String, Object> value) {
      this.values = value;
      return this;
    }

    public LintRequest build() {
      return new LintRequest(chartPath, strict, quiet, withSubcharts, values);
    }
  }

  private static <T> Map<String, T> copyMap(Map<String, T> value) {
    if (value == null || value.isEmpty())
      return Map.of();
    return Map.copyOf(new LinkedHashMap<>(value));
  }
}
