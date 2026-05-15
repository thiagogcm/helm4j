package dev.nthings.helm4j.release;

import dev.nthings.helm4j.internal.model.ModelSupport;

import org.jspecify.annotations.Nullable;

/** Request parameters for viewing release history. */
public record ReleaseHistory(@Nullable String releaseName, @Nullable String namespace, int max) {

  public ReleaseHistory {
    releaseName = ModelSupport.normalizeBlankToNull(releaseName);
    namespace = ModelSupport.normalizeBlankToNull(namespace);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private @Nullable String releaseName;
    private @Nullable String namespace;
    private int max;

    private Builder() {}

    public Builder releaseName(String value) {
      this.releaseName = value;
      return this;
    }

    public Builder namespace(String value) {
      this.namespace = value;
      return this;
    }

    public Builder max(int value) {
      this.max = value;
      return this;
    }

    public ReleaseHistory build() {
      return new ReleaseHistory(releaseName, namespace, max);
    }
  }
}
