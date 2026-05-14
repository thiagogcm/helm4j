package dev.nthings.helm4j.repo;

import dev.nthings.helm4j.internal.api.Invocations;
import dev.nthings.helm4j.internal.gateway.RepoGateway;
import dev.nthings.helm4j.internal.model.ModelSupport;

import org.jspecify.annotations.Nullable;

/** Request parameters for registry login. */
public record RegistryLoginRequest(
    @Nullable String hostname,
    @Nullable String username,
    @Nullable String password,
    @Nullable String certificateFile,
    @Nullable String keyFile,
    @Nullable String certificateAuthorityFile,
    boolean insecure,
    boolean plainHttp) {

  public RegistryLoginRequest {
    hostname = ModelSupport.normalizeBlankToNull(hostname);
    username = ModelSupport.normalizeBlankToNull(username);
    password = ModelSupport.normalizeBlankToNull(password);
    certificateFile = ModelSupport.normalizeBlankToNull(certificateFile);
    keyFile = ModelSupport.normalizeBlankToNull(keyFile);
    certificateAuthorityFile = ModelSupport.normalizeBlankToNull(certificateAuthorityFile);
  }

  public static Builder builder() {
    return new Builder(null);
  }

  static Builder builder(RepoGateway gateway) {
    return new Builder(gateway);
  }

  public static final class Builder {
    private final RepoGateway gateway;
    private String hostname;
    private String username;
    private String password;
    private String certificateFile;
    private String keyFile;
    private String certificateAuthorityFile;
    private boolean insecure;
    private boolean plainHttp;

    private Builder(RepoGateway gateway) {
      this.gateway = gateway;
    }

    public Builder hostname(String value) {
      this.hostname = value;
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

    public Builder insecure(boolean value) {
      this.insecure = value;
      return this;
    }

    public Builder plainHttp(boolean value) {
      this.plainHttp = value;
      return this;
    }

    public RegistryLoginRequest build() {
      return new RegistryLoginRequest(
          hostname,
          username,
          password,
          certificateFile,
          keyFile,
          certificateAuthorityFile,
          insecure,
          plainHttp);
    }

    /** Builds the request and logs in to the registry through the bound client. */
    public RegistryResult execute() {
      return Invocations.requireBound(gateway).registryLogin(build());
    }
  }
}
