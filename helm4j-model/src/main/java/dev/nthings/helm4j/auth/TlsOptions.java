package dev.nthings.helm4j.auth;

import dev.nthings.helm4j.internal.model.ModelSupport;

import org.jspecify.annotations.Nullable;

/**
 * Transport security for a chart repository or OCI registry: client certificate, CA, and the
 * insecure/plain-HTTP escape hatches. Use {@link #none()} for verified TLS over HTTPS.
 */
public record TlsOptions(
    @Nullable String certificateFile,
    @Nullable String keyFile,
    @Nullable String certificateAuthorityFile,
    boolean insecureSkipTlsVerification,
    boolean plainHttp) {

  private static final TlsOptions NONE = new TlsOptions(null, null, null, false, false);

  public TlsOptions {
    certificateFile = ModelSupport.normalizeBlankToNull(certificateFile);
    keyFile = ModelSupport.normalizeBlankToNull(keyFile);
    certificateAuthorityFile = ModelSupport.normalizeBlankToNull(certificateAuthorityFile);
  }

  /** Default transport: system trust store, verified TLS, HTTPS. */
  public static TlsOptions none() {
    return NONE;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private @Nullable String certificateFile;
    private @Nullable String keyFile;
    private @Nullable String certificateAuthorityFile;
    private boolean insecureSkipTlsVerification;
    private boolean plainHttp;

    private Builder() {}

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

    public Builder insecureSkipTlsVerification(boolean value) {
      this.insecureSkipTlsVerification = value;
      return this;
    }

    public Builder plainHttp(boolean value) {
      this.plainHttp = value;
      return this;
    }

    public TlsOptions build() {
      return new TlsOptions(
          certificateFile,
          keyFile,
          certificateAuthorityFile,
          insecureSkipTlsVerification,
          plainHttp);
    }
  }
}
