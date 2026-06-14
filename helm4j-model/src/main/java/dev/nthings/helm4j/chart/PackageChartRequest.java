package dev.nthings.helm4j.chart;

import java.nio.file.Path;

import dev.nthings.helm4j.auth.TlsOptions;
import dev.nthings.helm4j.internal.model.ModelSupport;

import org.jspecify.annotations.Nullable;

/** Request parameters for packaging a chart directory. */
public record PackageChartRequest(
    @Nullable Path chartPath,
    @Nullable String version,
    @Nullable String appVersion,
    @Nullable Path destination,
    boolean dependencyUpdate,
    boolean sign,
    @Nullable String key,
    @Nullable String keyring,
    @Nullable String passphraseFile,
    TlsOptions tls) {

  public PackageChartRequest {
    chartPath = chartPath == null ? null : chartPath.toAbsolutePath();
    version = ModelSupport.normalizeBlankToNull(version);
    appVersion = ModelSupport.normalizeBlankToNull(appVersion);
    destination = destination == null ? null : destination.toAbsolutePath();
    key = ModelSupport.normalizeBlankToNull(key);
    keyring = ModelSupport.normalizeBlankToNull(keyring);
    passphraseFile = ModelSupport.normalizeBlankToNull(passphraseFile);
    tls = tls == null ? TlsOptions.none() : tls;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private @Nullable Path chartPath;
    private @Nullable String version;
    private @Nullable String appVersion;
    private @Nullable Path destination;
    private boolean dependencyUpdate;
    private boolean sign;
    private @Nullable String key;
    private @Nullable String keyring;
    private @Nullable String passphraseFile;
    private TlsOptions tls = TlsOptions.none();

    private Builder() {}

    public Builder chartPath(Path value) {
      this.chartPath = value;
      return this;
    }

    public Builder version(String value) {
      this.version = value;
      return this;
    }

    public Builder appVersion(String value) {
      this.appVersion = value;
      return this;
    }

    public Builder destination(Path value) {
      this.destination = value;
      return this;
    }

    public Builder dependencyUpdate(boolean value) {
      this.dependencyUpdate = value;
      return this;
    }

    public Builder sign(boolean value) {
      this.sign = value;
      return this;
    }

    public Builder key(String value) {
      this.key = value;
      return this;
    }

    public Builder keyring(String value) {
      this.keyring = value;
      return this;
    }

    public Builder passphraseFile(String value) {
      this.passphraseFile = value;
      return this;
    }

    public Builder tls(TlsOptions value) {
      this.tls = value;
      return this;
    }

    public PackageChartRequest build() {
      return new PackageChartRequest(
          chartPath,
          version,
          appVersion,
          destination,
          dependencyUpdate,
          sign,
          key,
          keyring,
          passphraseFile,
          tls);
    }
  }
}
