package dev.nthings.helm4j.release;

/** Request parameters for checking release status. */
public record StatusRequest(String releaseName, String namespace, int revision) {

  public StatusRequest {
    releaseName = normalize(releaseName);
    namespace = normalize(namespace);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private String releaseName;
    private String namespace;
    private int revision;

    private Builder() {
    }

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

  private static String normalize(String value) {
    if (value == null)
      return null;
    var normalized = value.trim();
    return normalized.isEmpty() ? null : normalized;
  }
}
