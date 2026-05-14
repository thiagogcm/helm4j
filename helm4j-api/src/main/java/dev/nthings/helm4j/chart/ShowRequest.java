package dev.nthings.helm4j.chart;

import java.util.Objects;
import java.util.function.Consumer;

import dev.nthings.helm4j.internal.api.Invocations;
import dev.nthings.helm4j.internal.gateway.ChartGateway;
import dev.nthings.helm4j.internal.model.ModelSupport;

import org.jspecify.annotations.Nullable;

/** Shared request options for Helm show operations. */
public record ShowRequest(ChartSource source, @Nullable String valuesJsonPath) {

  public ShowRequest {
    source = Objects.requireNonNullElseGet(source, ChartSource::defaults);
    valuesJsonPath = ModelSupport.normalizeBlankToNull(valuesJsonPath);
  }

  public static Builder builder() {
    return new Builder(null, null, null);
  }

  static Builder builder(ChartGateway gateway, ShowMode mode, ChartRef chart) {
    return new Builder(gateway, mode, chart);
  }

  public static final class Builder {
    private final @Nullable ChartGateway gateway;
    private final @Nullable ShowMode mode;
    private final @Nullable ChartRef chart;
    private final ChartSource.Builder sourceBuilder = ChartSource.builder();
    private @Nullable ChartSource source;
    private @Nullable String valuesJsonPath;

    private Builder(
        @Nullable ChartGateway gateway, @Nullable ShowMode mode, @Nullable ChartRef chart) {
      this.gateway = gateway;
      this.mode = mode;
      this.chart = chart;
    }

    public Builder source(ChartSource value) {
      this.source = value;
      return this;
    }

    public Builder source(Consumer<ChartSource.Builder> spec) {
      spec.accept(sourceBuilder);
      return this;
    }

    public Builder valuesJsonPath(String value) {
      this.valuesJsonPath = value;
      return this;
    }

    public ShowRequest build() {
      var resolvedSource =
          source != null ? source.merge(sourceBuilder.build()) : sourceBuilder.build();
      return new ShowRequest(resolvedSource, valuesJsonPath);
    }

    /** Builds the request and runs the show operation through the bound client. */
    public ShowResult execute() {
      return Invocations.requireBound(gateway)
          .show(
              Objects.requireNonNull(mode, "mode"),
              Objects.requireNonNull(chart, "chart"),
              build());
    }
  }
}
