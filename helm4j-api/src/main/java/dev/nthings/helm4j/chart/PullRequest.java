package dev.nthings.helm4j.chart;

import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

/** Request parameters for pulling a chart archive. */
public record PullRequest(
    ChartRef chart,
    ChartSource source,
    boolean untar,
    @Nullable Path untarDirectory,
    @Nullable Path destinationDirectory) {

  public PullRequest {
    chart = Objects.requireNonNull(chart, "chart");
    source = Objects.requireNonNullElseGet(source, ChartSource::defaults);
    untarDirectory = absoluteOrNull(untarDirectory);
    destinationDirectory = absoluteOrNull(destinationDirectory);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private final ChartSource.Builder sourceBuilder = ChartSource.builder();
    private @Nullable ChartRef chart;
    private @Nullable ChartSource source;
    private boolean untar;
    private @Nullable Path untarDirectory;
    private @Nullable Path destinationDirectory;

    private Builder() {}

    public Builder chart(ChartRef value) {
      this.chart = value;
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
          Objects.requireNonNull(chart, "chart"),
          resolvedSource,
          untar,
          untarDirectory,
          destinationDirectory);
    }
  }

  private static @Nullable Path absoluteOrNull(@Nullable Path value) {
    if (value == null) {
      return null;
    }
    return value.toAbsolutePath();
  }
}
