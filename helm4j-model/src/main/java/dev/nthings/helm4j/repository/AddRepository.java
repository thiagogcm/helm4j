package dev.nthings.helm4j.repository;

import java.time.Duration;

import dev.nthings.helm4j.auth.Credentials;
import dev.nthings.helm4j.auth.TlsOptions;
import dev.nthings.helm4j.internal.model.ModelSupport;

import org.jspecify.annotations.Nullable;

/** Request parameters for adding a chart repository. */
public record AddRepository(
    @Nullable String name,
    @Nullable String url,
    Credentials credentials,
    TlsOptions tls,
    boolean passCredentialsToAllHosts,
    boolean forceUpdate,
    boolean allowDeprecatedRepositories,
    @Nullable Duration timeout) {

  public AddRepository {
    name = ModelSupport.normalizeBlankToNull(name);
    url = ModelSupport.normalizeBlankToNull(url);
    credentials = credentials == null ? Credentials.none() : credentials;
    tls = tls == null ? TlsOptions.none() : tls;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private @Nullable String name;
    private @Nullable String url;
    private Credentials credentials = Credentials.none();
    private TlsOptions tls = TlsOptions.none();
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

    public Builder credentials(Credentials value) {
      this.credentials = value;
      return this;
    }

    public Builder tls(TlsOptions value) {
      this.tls = value;
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

    public AddRepository build() {
      return new AddRepository(
          name,
          url,
          credentials,
          tls,
          passCredentialsToAllHosts,
          forceUpdate,
          allowDeprecatedRepositories,
          timeout);
    }
  }
}
