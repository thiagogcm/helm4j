package dev.nthings.helm4j.repo;

import java.time.Duration;

/** Request parameters for adding a chart repository. */
public record RepoAddRequest(
    String name,
    String url,
    String username,
    String password,
    String certificateFile,
    String keyFile,
    String certificateAuthorityFile,
    boolean insecureSkipTlsVerification,
    boolean passCredentialsToAllHosts,
    boolean forceUpdate,
    boolean allowDeprecatedRepositories,
    Duration timeout) {

  public RepoAddRequest {
    name = normalize(name);
    url = normalize(url);
    username = normalize(username);
    password = normalize(password);
    certificateFile = normalize(certificateFile);
    keyFile = normalize(keyFile);
    certificateAuthorityFile = normalize(certificateAuthorityFile);
  }

  public static RepoAddRequest defaults() {
    return builder().build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private String name;
    private String url;
    private String username;
    private String password;
    private String certificateFile;
    private String keyFile;
    private String certificateAuthorityFile;
    private boolean insecureSkipTlsVerification;
    private boolean passCredentialsToAllHosts;
    private boolean forceUpdate;
    private boolean allowDeprecatedRepositories;
    private Duration timeout;

    private Builder() {}

    public Builder name(String value) {
      this.name = value;
      return this;
    }

    public Builder url(String value) {
      this.url = value;
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

    public Builder passCredentialsToAllHosts(boolean value) {
      this.passCredentialsToAllHosts = value;
      return this;
    }

    public Builder forceUpdate(boolean value) {
      this.forceUpdate = value;
      return this;
    }

    public Builder allowDeprecatedRepositories(boolean value) {
      this.allowDeprecatedRepositories = value;
      return this;
    }

    public Builder timeout(Duration value) {
      this.timeout = value;
      return this;
    }

    public RepoAddRequest build() {
      return new RepoAddRequest(
          name,
          url,
          username,
          password,
          certificateFile,
          keyFile,
          certificateAuthorityFile,
          insecureSkipTlsVerification,
          passCredentialsToAllHosts,
          forceUpdate,
          allowDeprecatedRepositories,
          timeout);
    }
  }

  private static String normalize(String value) {
    if (value == null) {
      return null;
    }
    var normalized = value.trim();
    return normalized.isEmpty() ? null : normalized;
  }
}
