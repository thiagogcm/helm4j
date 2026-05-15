package dev.nthings.helm4j.release;

import java.time.Duration;
import java.util.List;

import dev.nthings.helm4j.internal.model.ModelSupport;

import org.jspecify.annotations.Nullable;

/** Request parameters for running release tests. */
public record TestRelease(
    @Nullable String releaseName,
    @Nullable String namespace,
    @Nullable Duration timeout,
    List<String> filter) {

  public TestRelease {
    releaseName = ModelSupport.normalizeBlankToNull(releaseName);
    namespace = ModelSupport.normalizeBlankToNull(namespace);
    filter = ModelSupport.immutableListOrEmpty(filter);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private @Nullable String releaseName;
    private @Nullable String namespace;
    private @Nullable Duration timeout;
    private @Nullable List<String> filter;

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

    public TestRelease build() {
      return new TestRelease(releaseName, namespace, timeout, filter);
    }
  }
}
