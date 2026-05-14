package dev.nthings.helm4j.release;

import dev.nthings.helm4j.internal.model.ModelSupport;

/** Request parameters for checking release status. */
public record StatusRequest(String releaseName, String namespace, int revision) {

  public StatusRequest {
    releaseName = ModelSupport.normalizeBlankToNull(releaseName);
    namespace = ModelSupport.normalizeBlankToNull(namespace);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private String releaseName;
    private String namespace;
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

    public StatusRequest build() {
      return new StatusRequest(releaseName, namespace, revision);
    }
  }
}
