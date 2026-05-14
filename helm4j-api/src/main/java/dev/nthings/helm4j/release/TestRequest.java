package dev.nthings.helm4j.release;

import java.time.Duration;
import java.util.List;

import dev.nthings.helm4j.internal.model.ModelSupport;

/** Request parameters for running release tests. */
public record TestRequest(
    String releaseName, String namespace, Duration timeout, List<String> filter) {

  public TestRequest {
    releaseName = ModelSupport.normalizeBlankToNull(releaseName);
    namespace = ModelSupport.normalizeBlankToNull(namespace);
    filter = ModelSupport.immutableListOrEmpty(filter);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private String releaseName;
    private String namespace;
    private Duration timeout;
    private List<String> filter;

    private Builder() {}

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
      return new TestRequest(releaseName, namespace, timeout, filter);
    }
  }
}
