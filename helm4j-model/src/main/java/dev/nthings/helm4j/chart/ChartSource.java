package dev.nthings.helm4j.chart;

import dev.nthings.helm4j.auth.Credentials;
import dev.nthings.helm4j.auth.TlsOptions;
import dev.nthings.helm4j.internal.model.ModelSupport;

import org.jspecify.annotations.Nullable;

/**
 * How to fetch a chart: the repository URL, credentials, TLS, and signing options shared across
 * chart-consuming operations.
 *
 * <p>The chart identity and version live on {@link ChartRef}; this type carries only the transport
 * and resolution concerns layered on top of it.
 */
public record ChartSource(
    @Nullable String repositoryUrl,
    Credentials credentials,
    TlsOptions tls,
    @Nullable String keyringPath,
    boolean passCredentialsToAllHosts,
    boolean verifySignatures,
    boolean includePreReleaseVersions) {

  public ChartSource {
    repositoryUrl = ModelSupport.normalizeBlankToNull(repositoryUrl);
    credentials = credentials == null ? Credentials.none() : credentials;
    tls = tls == null ? TlsOptions.none() : tls;
    keyringPath = ModelSupport.normalizeBlankToNull(keyringPath);
  }

  public static ChartSource defaults() {
    return builder().build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private @Nullable String repositoryUrl;
    private Credentials credentials = Credentials.none();
    private TlsOptions tls = TlsOptions.none();
    private @Nullable String keyringPath;
    private boolean passCredentialsToAllHosts;
    private boolean verifySignatures;
    private boolean includePreReleaseVersions;

    private Builder() {}

    public Builder repositoryUrl(String value) {
      this.repositoryUrl = value;
      return this;
    }

    public Builder credentials(Credentials value) {
      this.credentials = value;
      return this;
    }

    public Builder tls(TlsOptions value) {
      this.tls = value;
      return this;
    }

    public Builder keyringPath(String value) {
      this.keyringPath = value;
      return this;
    }

    public Builder passCredentialsToAllHosts(boolean value) {
      this.passCredentialsToAllHosts = value;
      return this;
    }

    public Builder verifySignatures(boolean value) {
      this.verifySignatures = value;
      return this;
    }

    public Builder includePreReleaseVersions(boolean value) {
      this.includePreReleaseVersions = value;
      return this;
    }

    public ChartSource build() {
      return new ChartSource(
          repositoryUrl,
          credentials,
          tls,
          keyringPath,
          passCredentialsToAllHosts,
          verifySignatures,
          includePreReleaseVersions);
    }
  }
}
