package dev.nthings.helm4j.release;

import java.time.Duration;
import java.util.List;

import dev.nthings.helm4j.internal.api.Invocations;
import dev.nthings.helm4j.internal.gateway.ReleaseGateway;
import dev.nthings.helm4j.internal.model.ModelSupport;

import org.jspecify.annotations.Nullable;

/** Request parameters for running release tests. */
public record TestRequest(
    @Nullable String releaseName,
    @Nullable String namespace,
    @Nullable Duration timeout,
    List<String> filter) {

  public TestRequest {
    releaseName = ModelSupport.normalizeBlankToNull(releaseName);
    namespace = ModelSupport.normalizeBlankToNull(namespace);
    filter = ModelSupport.immutableListOrEmpty(filter);
  }

  public static Builder builder() {
    return new Builder(null);
  }

  static Builder builder(ReleaseGateway gateway) {
    return new Builder(gateway);
  }

  public static final class Builder {
    private final @Nullable ReleaseGateway gateway;
    private @Nullable String releaseName;
    private @Nullable String namespace;
    private @Nullable Duration timeout;
    private @Nullable List<String> filter;

    private Builder(@Nullable ReleaseGateway gateway) {
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

    public Builder timeout(Duration value) {
      this.timeout = value;
      return this;
    }

    public Builder filter(List<String> value) {
      this.filter = value;
      return this;
    }

    public TestRequest build() {
      return new TestRequest(
          releaseName, namespace, timeout, ModelSupport.immutableListOrEmpty(filter));
    }

    /** Builds the request and runs release tests through the bound client. */
    public TestResult execute() {
      return Invocations.requireBound(gateway).test(build());
    }
  }
}
