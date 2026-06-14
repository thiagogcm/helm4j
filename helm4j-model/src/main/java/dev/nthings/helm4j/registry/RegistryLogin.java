package dev.nthings.helm4j.registry;

import dev.nthings.helm4j.auth.Credentials;
import dev.nthings.helm4j.auth.TlsOptions;
import dev.nthings.helm4j.internal.model.ModelSupport;

import org.jspecify.annotations.Nullable;

/** Request parameters for registry login. */
public record RegistryLogin(@Nullable String hostname, Credentials credentials, TlsOptions tls) {

  public RegistryLogin {
    hostname = ModelSupport.normalizeBlankToNull(hostname);
    credentials = credentials == null ? Credentials.none() : credentials;
    tls = tls == null ? TlsOptions.none() : tls;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private @Nullable String hostname;
    private Credentials credentials = Credentials.none();
    private TlsOptions tls = TlsOptions.none();

    private Builder() {}

    public Builder hostname(String value) {
      this.hostname = value;
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

    public RegistryLogin build() {
      return new RegistryLogin(hostname, credentials, tls);
    }
  }
}
