package dev.nthings.helm4j.chart;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

import dev.nthings.helm4j.internal.api.Invocations;
import dev.nthings.helm4j.internal.gateway.ChartGateway;
import dev.nthings.helm4j.internal.model.ModelSupport;

/** Request parameters for linting a chart. */
public record LintRequest(
    Path chartPath,
    boolean strict,
    boolean quiet,
    boolean withSubcharts,
    Map<String, Object> values) {

  public LintRequest {
    chartPath = Objects.requireNonNull(chartPath, "chartPath").toAbsolutePath();
    values = ModelSupport.immutableMapOrEmpty(values);
  }

  public static Builder builder() {
    return new Builder(null);
  }

  static Builder builder(ChartGateway gateway) {
    return new Builder(gateway);
  }

  public static final class Builder {
    private final ChartGateway gateway;
    private Path chartPath;
    private boolean strict;
    private boolean quiet;
    private boolean withSubcharts;
    private Map<String, Object> values;

    private Builder(ChartGateway gateway) {
      this.gateway = gateway;
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

    /** Builds the request and lints the chart through the bound client. */
    public LintResult execute() {
      return Invocations.requireBound(gateway).lint(build());
    }
  }
}
