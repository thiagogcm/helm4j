package dev.nthings.helm4j.chart;

import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;

import dev.nthings.helm4j.internal.api.Invocations;
import dev.nthings.helm4j.internal.gateway.ChartGateway;
import dev.nthings.helm4j.internal.model.ModelSupport;

/** Request parameters for pulling a chart archive. */
public record PullRequest(
    String chartReference,
    ChartSource source,
    boolean untar,
    Path untarDirectory,
    Path destinationDirectory) {

  public PullRequest {
    chartReference = ModelSupport.normalizeBlankToNull(chartReference);
    source = Objects.requireNonNullElseGet(source, ChartSource::defaults);
    untarDirectory = absoluteOrNull(untarDirectory);
    destinationDirectory = absoluteOrNull(destinationDirectory);
  }

  public static Builder builder() {
    return new Builder(null);
  }

  static Builder builder(ChartGateway gateway) {
    return new Builder(gateway);
  }

  public static final class Builder {
    private final ChartGateway gateway;
    private final ChartSource.Builder sourceBuilder = ChartSource.builder();
    private String chartReference;
    private ChartSource source;
    private boolean untar;
    private Path untarDirectory;
    private Path destinationDirectory;

    private Builder(ChartGateway gateway) {
      this.gateway = gateway;
    }

    public Builder chartReference(String value) {
      this.chartReference = value;
      return this;
    }

    public Builder source(ChartSource value) {
      this.source = value;
      return this;
    }

    public Builder source(Consumer<ChartSource.Builder> consumer) {
      consumer.accept(sourceBuilder);
      return this;
    }

    public Builder untar(boolean value) {
      this.untar = value;
      return this;
    }

    public Builder untarDirectory(Path value) {
      this.untarDirectory = value;
      return this;
    }

    public Builder destinationDirectory(Path value) {
      this.destinationDirectory = value;
      return this;
    }

    public PullRequest build() {
      var resolvedSource =
          source != null ? source.merge(sourceBuilder.build()) : sourceBuilder.build();
      return new PullRequest(
          chartReference, resolvedSource, untar, untarDirectory, destinationDirectory);
    }

    /** Builds the request and pulls the chart through the bound client. */
    public PullResult execute() {
      return Invocations.requireBound(gateway).pull(build());
    }
  }

  private static Path absoluteOrNull(Path value) {
    if (value == null) {
      return null;
    }
    return value.toAbsolutePath();
  }
}
