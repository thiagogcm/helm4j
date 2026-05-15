package dev.nthings.helm4j.release;

import dev.nthings.helm4j.internal.model.ModelSupport;

import org.jspecify.annotations.Nullable;

/** Request parameters for checking release status. */
public record StatusRelease(
    @Nullable String releaseName, @Nullable String namespace, int revision) {

  public StatusRelease {
    releaseName = ModelSupport.normalizeBlankToNull(releaseName);
    namespace = ModelSupport.normalizeBlankToNull(namespace);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private @Nullable String releaseName;
    private @Nullable String namespace;
    private int revision;

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

    public StatusRelease build() {
      return new StatusRelease(releaseName, namespace, revision);
    }
  }
}
