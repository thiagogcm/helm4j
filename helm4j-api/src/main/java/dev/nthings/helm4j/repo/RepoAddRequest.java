package dev.nthings.helm4j.repo;

import java.time.Duration;

import dev.nthings.helm4j.internal.model.ModelSupport;

import org.jspecify.annotations.Nullable;

/** Request parameters for adding a chart repository. */
public record RepoAddRequest(
    @Nullable String name,
    @Nullable String url,
    @Nullable String username,
    @Nullable String password,
    @Nullable String certificateFile,
    @Nullable String keyFile,
    @Nullable String certificateAuthorityFile,
    boolean insecureSkipTlsVerification,
    boolean passCredentialsToAllHosts,
    boolean forceUpdate,
    boolean allowDeprecatedRepositories,
    @Nullable Duration timeout) {

  public RepoAddRequest {
    name = ModelSupport.normalizeBlankToNull(name);
    url = ModelSupport.normalizeBlankToNull(url);
    username = ModelSupport.normalizeBlankToNull(username);
    password = ModelSupport.normalizeBlankToNull(password);
    certificateFile = ModelSupport.normalizeBlankToNull(certificateFile);
    keyFile = ModelSupport.normalizeBlankToNull(keyFile);
    certificateAuthorityFile = ModelSupport.normalizeBlankToNull(certificateAuthorityFile);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private @Nullable String name;
    private @Nullable String url;
    private @Nullable String username;
    private @Nullable String password;
    private @Nullable String certificateFile;
    private @Nullable String keyFile;
    private @Nullable String certificateAuthorityFile;
    private boolean insecureSkipTlsVerification;
    private boolean passCredentialsToAllHosts;
    private boolean forceUpdate;
    private boolean allowDeprecatedRepositories;
    private @Nullable Duration timeout;

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
}
