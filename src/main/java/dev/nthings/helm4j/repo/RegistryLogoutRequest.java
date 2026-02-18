package dev.nthings.helm4j.repo;

/** Request parameters for registry logout. */
public record RegistryLogoutRequest(String hostname) {

  public RegistryLogoutRequest {
    hostname = normalize(hostname);
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

  private static String normalize(String value) {
    if (value == null) {
      return null;
    }
    var normalized = value.trim();
    return normalized.isEmpty() ? null : normalized;
  }
}
