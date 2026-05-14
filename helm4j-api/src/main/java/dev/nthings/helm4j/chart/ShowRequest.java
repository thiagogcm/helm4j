package dev.nthings.helm4j.chart;

import java.util.Objects;
import java.util.function.Consumer;

import dev.nthings.helm4j.internal.model.ModelSupport;

import org.jspecify.annotations.Nullable;

/** Shared request options for Helm show operations. */
public record ShowRequest(ChartSource source, @Nullable String valuesJsonPath) {

  public ShowRequest {
    source = Objects.requireNonNullElseGet(source, ChartSource::defaults);
    valuesJsonPath = ModelSupport.normalizeBlankToNull(valuesJsonPath);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private final ChartSource.Builder sourceBuilder = ChartSource.builder();
    private @Nullable ChartSource source;
    private @Nullable String valuesJsonPath;

    private Builder() {}

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
  }
}
