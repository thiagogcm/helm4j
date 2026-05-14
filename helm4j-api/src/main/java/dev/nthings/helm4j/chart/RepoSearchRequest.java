package dev.nthings.helm4j.chart;

import dev.nthings.helm4j.internal.model.ModelSupport;

import org.jspecify.annotations.Nullable;

/** Request parameters for searching configured chart repositories. */
public record RepoSearchRequest(
    @Nullable String keyword,
    boolean regularExpression,
    boolean includeAllVersions,
    boolean includePreReleaseVersions,
    @Nullable String versionConstraint,
    boolean failIfNoResults,
    int maxColumnWidth) {

  public RepoSearchRequest {
    keyword = ModelSupport.normalizeBlankToNull(keyword);
    versionConstraint = ModelSupport.normalizeBlankToNull(versionConstraint);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private @Nullable String keyword;
    private boolean regularExpression;
    private boolean includeAllVersions;
    private boolean includePreReleaseVersions;
    private @Nullable String versionConstraint;
    private boolean failIfNoResults;
    private int maxColumnWidth;

    private Builder() {}

    public Builder keyword(String value) {
      this.keyword = value;
      return this;
    }

    public Builder regularExpression(boolean value) {
      this.regularExpression = value;
      return this;
    }

    public Builder includeAllVersions(boolean value) {
      this.includeAllVersions = value;
      return this;
    }

    public Builder includePreReleaseVersions(boolean value) {
      this.includePreReleaseVersions = value;
      return this;
    }

    public Builder versionConstraint(String value) {
      this.versionConstraint = value;
      return this;
    }

    public Builder failIfNoResults(boolean value) {
      this.failIfNoResults = value;
      return this;
    }

    public Builder maxColumnWidth(int value) {
      this.maxColumnWidth = value;
      return this;
    }

    public RepoSearchRequest build() {
      return new RepoSearchRequest(
          keyword,
          regularExpression,
          includeAllVersions,
          includePreReleaseVersions,
          versionConstraint,
          failIfNoResults,
          maxColumnWidth);
    }
  }
}
