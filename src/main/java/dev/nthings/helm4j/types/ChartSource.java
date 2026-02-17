package dev.nthings.helm4j.types;

import java.util.Objects;

/** Shared chart resolution, TLS, and auth options across chart-consuming operations. */
public record ChartSource(
    String version,
    String repositoryUrl,
    String username,
    String password,
    boolean plainHttp,
    boolean insecureSkipTlsVerification,
    String keyringPath,
    String certificateFile,
    String keyFile,
    String certificateAuthorityFile,
    boolean passCredentialsToAllHosts,
    boolean verifySignatures,
    boolean includePreReleaseVersions) {

  public ChartSource {
    version = normalize(version);
    repositoryUrl = normalize(repositoryUrl);
    username = normalize(username);
    password = normalize(password);
    keyringPath = normalize(keyringPath);
    certificateFile = normalize(certificateFile);
    keyFile = normalize(keyFile);
    certificateAuthorityFile = normalize(certificateAuthorityFile);
  }

  public static ChartSource defaults() {
    return builder().build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private String version;
    private String repositoryUrl;
    private String username;
    private String password;
    private boolean plainHttp;
    private boolean insecureSkipTlsVerification;
    private String keyringPath;
    private String certificateFile;
    private String keyFile;
    private String certificateAuthorityFile;
    private boolean passCredentialsToAllHosts;
    private boolean verifySignatures;
    private boolean includePreReleaseVersions;

    private Builder() {}

    public Builder version(String value) {
      this.version = value;
      return this;
    }

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
          version,
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

  private static String normalize(String value) {
    if (value == null) {
      return null;
    }
    var normalized = value.trim();
    return normalized.isEmpty() ? null : normalized;
  }

  public ChartSource merge(ChartSource overrides) {
    Objects.requireNonNull(overrides, "overrides");
    return new ChartSource(
        coalesce(overrides.version, version),
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

  private static String coalesce(String first, String second) {
    return first == null ? second : first;
  }
}
