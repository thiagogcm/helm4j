package dev.nthings.helm4j.repo;

import dev.nthings.helm4j.internal.model.ModelSupport;

/** Request parameters for registry logout. */
public record RegistryLogoutRequest(String hostname) {

  public RegistryLogoutRequest {
    hostname = ModelSupport.normalizeBlankToNull(hostname);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private String hostname;

    private Builder() {}

    public Builder hostname(String value) {
      this.hostname = value;
      return this;
    }

    public RegistryLogoutRequest build() {
      return new RegistryLogoutRequest(hostname);
    }
  }
}
