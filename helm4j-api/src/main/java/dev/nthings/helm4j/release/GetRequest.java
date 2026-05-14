package dev.nthings.helm4j.release;

import dev.nthings.helm4j.internal.model.ModelSupport;

/** Request parameters for getting release information. */
public record GetRequest(String releaseName, String namespace, int revision, boolean allValues) {

  public GetRequest {
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

    public GetRequest build() {
      return new GetRequest(releaseName, namespace, revision, allValues);
    }
  }
}
