package dev.nthings.helm4j.release;

import dev.nthings.helm4j.internal.api.Invocations;
import dev.nthings.helm4j.internal.gateway.ReleaseGateway;
import dev.nthings.helm4j.internal.model.ModelSupport;

import org.jspecify.annotations.Nullable;

/** Request parameters for checking release status. */
public record StatusRequest(
    @Nullable String releaseName, @Nullable String namespace, int revision) {

  public StatusRequest {
    releaseName = ModelSupport.normalizeBlankToNull(releaseName);
    namespace = ModelSupport.normalizeBlankToNull(namespace);
  }

  public static Builder builder() {
    return new Builder(null);
  }

  static Builder builder(ReleaseGateway gateway) {
    return new Builder(gateway);
  }

  public static final class Builder {
    private final ReleaseGateway gateway;
    private String releaseName;
    private String namespace;
    private int revision;

    private Builder(ReleaseGateway gateway) {
      this.gateway = gateway;
    }

    public Builder releaseName(String value) {
      this.releaseName = value;
      return this;
    }

    public Builder namespace(String value) {
      this.namespace = value;
      return this;
    }

    public Builder revision(int value) {
      this.revision = value;
      return this;
    }

    public StatusRequest build() {
      return new StatusRequest(releaseName, namespace, revision);
    }

    /** Builds the request and queries status through the bound client. */
    public StatusResult execute() {
      return Invocations.requireBound(gateway).status(build());
    }
  }
}
