package dev.nthings.helm4j.repo;

import dev.nthings.helm4j.internal.api.Invocations;
import dev.nthings.helm4j.internal.gateway.RepoGateway;
import dev.nthings.helm4j.internal.model.ModelSupport;

/** Request parameters for registry logout. */
public record RegistryLogoutRequest(String hostname) {

  public RegistryLogoutRequest {
    hostname = ModelSupport.normalizeBlankToNull(hostname);
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

    private Builder(RepoGateway gateway) {
      this.gateway = gateway;
    }

    public Builder hostname(String value) {
      this.hostname = value;
      return this;
    }

    public RegistryLogoutRequest build() {
      return new RegistryLogoutRequest(hostname);
    }

    /** Builds the request and logs out of the registry through the bound client. */
    public RegistryResult execute() {
      return Invocations.requireBound(gateway).registryLogout(build());
    }
  }
}
