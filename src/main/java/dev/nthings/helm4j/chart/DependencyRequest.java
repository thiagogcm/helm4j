package dev.nthings.helm4j.chart;

import java.nio.file.Path;

import dev.nthings.helm4j.internal.model.ModelSupport;

/** Request parameters for listing chart dependencies. */
public record DependencyRequest(
    Path chartPath,
    boolean skipRefresh,
    boolean verify,
    String keyring,
    boolean plainHttp,
    boolean insecureSkipTlsVerification,
    String certificateFile,
    String keyFile,
    String certificateAuthorityFile) {

  public DependencyRequest {
    chartPath = chartPath == null ? null : chartPath.toAbsolutePath();
    keyring = ModelSupport.normalizeBlankToNull(keyring);
    certificateFile = ModelSupport.normalizeBlankToNull(certificateFile);
    keyFile = ModelSupport.normalizeBlankToNull(keyFile);
    certificateAuthorityFile = ModelSupport.normalizeBlankToNull(certificateAuthorityFile);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private Path chartPath;
    private boolean skipRefresh;
    private boolean verify;
    private String keyring;
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

    public DependencyRequest build() {
      return new DependencyRequest(
          chartPath,
          skipRefresh,
          verify,
          keyring,
          plainHttp,
          insecureSkipTlsVerification,
          certificateFile,
          keyFile,
          certificateAuthorityFile);
    }
  }
}
