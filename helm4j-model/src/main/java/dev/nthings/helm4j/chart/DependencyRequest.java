package dev.nthings.helm4j.chart;

import java.nio.file.Path;

import dev.nthings.helm4j.auth.TlsOptions;
import dev.nthings.helm4j.internal.model.ModelSupport;

import org.jspecify.annotations.Nullable;

/** Request parameters for listing chart dependencies. */
public record DependencyRequest(
    @Nullable Path chartPath,
    boolean skipRefresh,
    boolean verify,
    @Nullable String keyring,
    TlsOptions tls) {

  public DependencyRequest {
    chartPath = chartPath == null ? null : chartPath.toAbsolutePath();
    keyring = ModelSupport.normalizeBlankToNull(keyring);
    tls = tls == null ? TlsOptions.none() : tls;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private @Nullable Path chartPath;
    private boolean skipRefresh;
    private boolean verify;
    private @Nullable String keyring;
    private TlsOptions tls = TlsOptions.none();

    private Builder() {}

    public Builder chartPath(Path value) {
      this.chartPath = value;
      return this;
    }

    public Builder skipRefresh(boolean value) {
      this.skipRefresh = value;
      return this;
    }

    public Builder verify(boolean value) {
      this.verify = value;
      return this;
    }

    public Builder keyring(String value) {
      this.keyring = value;
      return this;
    }

    public Builder tls(TlsOptions value) {
      this.tls = value;
      return this;
    }

    public DependencyRequest build() {
      return new DependencyRequest(chartPath, skipRefresh, verify, keyring, tls);
    }
  }
}
