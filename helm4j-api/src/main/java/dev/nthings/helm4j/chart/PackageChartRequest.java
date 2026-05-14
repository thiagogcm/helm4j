package dev.nthings.helm4j.chart;

import java.nio.file.Path;

import dev.nthings.helm4j.internal.api.Invocations;
import dev.nthings.helm4j.internal.gateway.ChartGateway;
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
    boolean plainHttp,
    boolean insecureSkipTlsVerification,
    @Nullable String certificateFile,
    @Nullable String keyFile,
    @Nullable String certificateAuthorityFile) {

  public PackageChartRequest {
    chartPath = chartPath == null ? null : chartPath.toAbsolutePath();
    version = ModelSupport.normalizeBlankToNull(version);
    appVersion = ModelSupport.normalizeBlankToNull(appVersion);
    destination = destination == null ? null : destination.toAbsolutePath();
    key = ModelSupport.normalizeBlankToNull(key);
    keyring = ModelSupport.normalizeBlankToNull(keyring);
    passphraseFile = ModelSupport.normalizeBlankToNull(passphraseFile);
    certificateFile = ModelSupport.normalizeBlankToNull(certificateFile);
    keyFile = ModelSupport.normalizeBlankToNull(keyFile);
    certificateAuthorityFile = ModelSupport.normalizeBlankToNull(certificateAuthorityFile);
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
    private String version;
    private String appVersion;
    private Path destination;
    private boolean dependencyUpdate;
    private boolean sign;
    private String key;
    private String keyring;
    private String passphraseFile;
    private boolean plainHttp;
    private boolean insecureSkipTlsVerification;
    private String certificateFile;
    private String keyFile;
    private String certificateAuthorityFile;

    private Builder(ChartGateway gateway) {
      this.gateway = gateway;
    }

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

    public Builder plainHttp(boolean value) {
      this.plainHttp = value;
      return this;
    }

    public Builder insecureSkipTlsVerification(boolean value) {
      this.insecureSkipTlsVerification = value;
      return this;
    }

    public Builder certificateFile(String value) {
      this.certificateFile = value;
      return this;
    }

    public Builder keyFile(String value) {
      this.keyFile = value;
      return this;
    }

    public Builder certificateAuthorityFile(String value) {
      this.certificateAuthorityFile = value;
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
          plainHttp,
          insecureSkipTlsVerification,
          certificateFile,
          keyFile,
          certificateAuthorityFile);
    }

    /** Builds the request and packages the chart through the bound client. */
    public PackageChartResult execute() {
      return Invocations.requireBound(gateway).packageChart(build());
    }
  }
}
