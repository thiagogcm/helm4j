package dev.nthings.helm4j.options;

/** Options accepted by {@code helm search repo}. */
public record SearchOptions(
    String query,
    boolean regularExpression,
    boolean includeAllVersions,
    boolean includePreReleaseVersions,
    String versionConstraint,
    boolean failIfNoResults) {

  public SearchOptions {
    query = normalize(query);
    versionConstraint = normalize(versionConstraint);
  }

  public static SearchOptions defaults() {
    return builder().build();
  }

  public static Builder builder() {
    return new Builder();
  }

  private static String normalize(String value) {
    if (value == null) {
      return null;
    }
    var trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  public static final class Builder {
    private String query;
    private boolean regularExpression;
    private boolean includeAllVersions;
    private boolean includePreReleaseVersions;
    private String versionConstraint;
    private boolean failIfNoResults;

    private Builder() {}

    public Builder query(String query) {
      this.query = query;
      return this;
    }

    public Builder regularExpression(boolean regularExpression) {
      this.regularExpression = regularExpression;
      return this;
    }

    public Builder includeAllVersions(boolean includeAllVersions) {
      this.includeAllVersions = includeAllVersions;
      return this;
    }

    public Builder includePreReleaseVersions(boolean includePreReleaseVersions) {
      this.includePreReleaseVersions = includePreReleaseVersions;
      return this;
    }

    public Builder versionConstraint(String versionConstraint) {
      this.versionConstraint = versionConstraint;
      return this;
    }

    public Builder failIfNoResults(boolean failIfNoResults) {
      this.failIfNoResults = failIfNoResults;
      return this;
    }

    public SearchOptions build() {
      return new SearchOptions(
          query,
          regularExpression,
          includeAllVersions,
          includePreReleaseVersions,
          versionConstraint,
          failIfNoResults);
    }
  }
}
