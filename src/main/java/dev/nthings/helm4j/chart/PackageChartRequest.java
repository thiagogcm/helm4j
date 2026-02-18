package dev.nthings.helm4j.chart;

import java.nio.file.Path;

import dev.nthings.helm4j.internal.model.ModelSupport;

/** Request parameters for packaging a chart directory. */
public record PackageChartRequest(
    Path chartPath,
    String version,
    String appVersion,
    Path destination,
    boolean dependencyUpdate,
    boolean sign,
    String key,
    String keyring,
    String passphraseFile,
    boolean plainHttp,
    boolean insecureSkipTlsVerification,
    String certificateFile,
    String keyFile,
    String certificateAuthorityFile) {

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
    return new Builder();
  }

  public static final class Builder {
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
  }
}
