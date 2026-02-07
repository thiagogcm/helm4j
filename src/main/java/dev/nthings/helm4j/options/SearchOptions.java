package dev.nthings.helm4j.options;

/** Options accepted by {@code helm search repo}. */
public final class SearchOptions {

  private final String query;
  private final Boolean regularExpression;
  private final Boolean includeAllVersions;
  private final Boolean includePreReleaseVersions;
  private final String versionConstraint;
  private final Boolean failIfNoResults;

  private SearchOptions(Builder builder) {
    this.query = builder.query;
    this.regularExpression = builder.regularExpression;
    this.includeAllVersions = builder.includeAllVersions;
    this.includePreReleaseVersions = builder.includePreReleaseVersions;
    this.versionConstraint = builder.versionConstraint;
    this.failIfNoResults = builder.failIfNoResults;
  }

  public static SearchOptions defaults() {
    return builder().build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public String query() {
    return query;
  }

  public Boolean regularExpression() {
    return regularExpression;
  }

  public Boolean includeAllVersions() {
    return includeAllVersions;
  }

  public Boolean includePreReleaseVersions() {
    return includePreReleaseVersions;
  }

  public String versionConstraint() {
    return versionConstraint;
  }

  public Boolean failIfNoResults() {
    return failIfNoResults;
  }

  public static final class Builder {
    private String query;
    private Boolean regularExpression;
    private Boolean includeAllVersions;
    private Boolean includePreReleaseVersions;
    private String versionConstraint;
    private Boolean failIfNoResults;

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
      return new SearchOptions(this);
    }
  }
}
