package dev.nthings.helm4j.chart;

import dev.nthings.helm4j.internal.model.ModelSupport;

import org.jspecify.annotations.Nullable;

/** Request parameters for pushing a packaged chart to an OCI registry. */
public record PushRequest(
    @Nullable String chartReference,
    @Nullable String remote,
    boolean plainHttp,
    boolean insecureSkipTlsVerification,
    @Nullable String certificateFile,
    @Nullable String keyFile,
    @Nullable String certificateAuthorityFile) {

  public PushRequest {
    chartReference = ModelSupport.normalizeBlankToNull(chartReference);
    remote = ModelSupport.normalizeBlankToNull(remote);
    certificateFile = ModelSupport.normalizeBlankToNull(certificateFile);
    keyFile = ModelSupport.normalizeBlankToNull(keyFile);
    certificateAuthorityFile = ModelSupport.normalizeBlankToNull(certificateAuthorityFile);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private @Nullable String chartReference;
    private @Nullable String remote;
    private boolean plainHttp;
    private boolean insecureSkipTlsVerification;
    private @Nullable String certificateFile;
    private @Nullable String keyFile;
    private @Nullable String certificateAuthorityFile;

    private Builder() {}

    public Builder chartReference(String value) {
      this.chartReference = value;
      return this;
    }

    public Builder remote(String value) {
      this.remote = value;
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

    public PushRequest build() {
      return new PushRequest(
          chartReference,
          remote,
          plainHttp,
          insecureSkipTlsVerification,
          certificateFile,
          keyFile,
          certificateAuthorityFile);
    }
  }
}
