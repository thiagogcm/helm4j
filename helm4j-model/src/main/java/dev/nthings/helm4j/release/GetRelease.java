package dev.nthings.helm4j.release;

import dev.nthings.helm4j.internal.model.ModelSupport;

import org.jspecify.annotations.Nullable;

/** Request parameters for getting release information. */
public record GetRelease(
    @Nullable String releaseName, @Nullable String namespace, int revision, boolean allValues) {

  public GetRelease {
    releaseName = ModelSupport.normalizeBlankToNull(releaseName);
    namespace = ModelSupport.normalizeBlankToNull(namespace);
  }

  public static Builder builder() {
    return new Builder();
  }

  /**
   * Fluent builder for the release {@code get} family. The same request shape feeds every variant;
   * the namespace client picks the variant.
   */
  public static final class Builder {
    private @Nullable String releaseName;
    private @Nullable String namespace;
    private int revision;
    private boolean allValues;

    private Builder() {}

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

    public Builder allValues(boolean value) {
      this.allValues = value;
      return this;
    }

    public GetRelease build() {
      return new GetRelease(releaseName, namespace, revision, allValues);
    }
  }
}
