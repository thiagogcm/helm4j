package dev.nthings.helm4j.chart;

import dev.nthings.helm4j.auth.TlsOptions;
import dev.nthings.helm4j.internal.model.ModelSupport;

import org.jspecify.annotations.Nullable;

/** Request parameters for pushing a packaged chart to an OCI registry. */
public record PushRequest(
    @Nullable String chartReference, @Nullable String remote, TlsOptions tls) {

  public PushRequest {
    chartReference = ModelSupport.normalizeBlankToNull(chartReference);
    remote = ModelSupport.normalizeBlankToNull(remote);
    tls = tls == null ? TlsOptions.none() : tls;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private @Nullable String chartReference;
    private @Nullable String remote;
    private TlsOptions tls = TlsOptions.none();

    private Builder() {}

    public Builder chartReference(String value) {
      this.chartReference = value;
      return this;
    }

    public Builder remote(String value) {
      this.remote = value;
      return this;
    }

    public Builder tls(TlsOptions value) {
      this.tls = value;
      return this;
    }

    public PushRequest build() {
      return new PushRequest(chartReference, remote, tls);
    }
  }
}
