package dev.nthings.helm4j.release;

import dev.nthings.helm4j.internal.model.ModelSupport;

/** Request parameters for viewing release history. */
public record HistoryRequest(String releaseName, String namespace, int max) {

  public HistoryRequest {
    releaseName = ModelSupport.normalizeBlankToNull(releaseName);
    namespace = ModelSupport.normalizeBlankToNull(namespace);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private String releaseName;
    private String namespace;
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

    public HistoryRequest build() {
      return new HistoryRequest(releaseName, namespace, max);
    }
  }
}
