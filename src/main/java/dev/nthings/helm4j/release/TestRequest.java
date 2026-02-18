package dev.nthings.helm4j.release;

import java.time.Duration;
import java.util.List;

/** Request parameters for running release tests. */
public record TestRequest(
    String releaseName, String namespace, Duration timeout, List<String> filter) {

  public TestRequest {
    releaseName = normalize(releaseName);
    namespace = normalize(namespace);
    filter = filter == null ? List.of() : List.copyOf(filter);
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

  private static String normalize(String value) {
    if (value == null) {
      return null;
    }
    var normalized = value.trim();
    return normalized.isEmpty() ? null : normalized;
  }
}
