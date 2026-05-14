package dev.nthings.helm4j.chart;

import java.util.Objects;

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
    @Nullable String username,
    @Nullable String password,
    boolean plainHttp,
    boolean insecureSkipTlsVerification,
    @Nullable String keyringPath,
    @Nullable String certificateFile,
    @Nullable String keyFile,
    @Nullable String certificateAuthorityFile,
    boolean passCredentialsToAllHosts,
    boolean verifySignatures,
    boolean includePreReleaseVersions) {

  public ChartSource {
    repositoryUrl = ModelSupport.normalizeBlankToNull(repositoryUrl);
    username = ModelSupport.normalizeBlankToNull(username);
    password = ModelSupport.normalizeBlankToNull(password);
    keyringPath = ModelSupport.normalizeBlankToNull(keyringPath);
    certificateFile = ModelSupport.normalizeBlankToNull(certificateFile);
    keyFile = ModelSupport.normalizeBlankToNull(keyFile);
    certificateAuthorityFile = ModelSupport.normalizeBlankToNull(certificateAuthorityFile);
  }

  public static ChartSource defaults() {
    return builder().build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private @Nullable String repositoryUrl;
    private @Nullable String username;
    private @Nullable String password;
    private boolean plainHttp;
    private boolean insecureSkipTlsVerification;
    private @Nullable String keyringPath;
    private @Nullable String certificateFile;
    private @Nullable String keyFile;
    private @Nullable String certificateAuthorityFile;
    private boolean passCredentialsToAllHosts;
    private boolean verifySignatures;
    private boolean includePreReleaseVersions;

    private Builder() {}

    public Builder repositoryUrl(String value) {
      this.repositoryUrl = value;
      return this;
    }

    public Builder username(String value) {
      this.username = value;
      return this;
    }

    public Builder password(String value) {
      this.password = value;
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

    public Builder keyringPath(String value) {
      this.keyringPath = value;
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
          username,
          password,
          plainHttp,
          insecureSkipTlsVerification,
          keyringPath,
          certificateFile,
          keyFile,
          certificateAuthorityFile,
          passCredentialsToAllHosts,
          verifySignatures,
          includePreReleaseVersions);
    }
  }

  public ChartSource merge(ChartSource overrides) {
    Objects.requireNonNull(overrides, "overrides");
    return new ChartSource(
        coalesce(overrides.repositoryUrl, repositoryUrl),
        coalesce(overrides.username, username),
        coalesce(overrides.password, password),
        overrides.plainHttp || plainHttp,
        overrides.insecureSkipTlsVerification || insecureSkipTlsVerification,
        coalesce(overrides.keyringPath, keyringPath),
        coalesce(overrides.certificateFile, certificateFile),
        coalesce(overrides.keyFile, keyFile),
        coalesce(overrides.certificateAuthorityFile, certificateAuthorityFile),
        overrides.passCredentialsToAllHosts || passCredentialsToAllHosts,
        overrides.verifySignatures || verifySignatures,
        overrides.includePreReleaseVersions || includePreReleaseVersions);
  }

  private static @Nullable String coalesce(@Nullable String first, @Nullable String second) {
    return first == null ? second : first;
  }
}
