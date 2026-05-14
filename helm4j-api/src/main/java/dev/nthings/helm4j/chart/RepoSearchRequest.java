package dev.nthings.helm4j.chart;

import dev.nthings.helm4j.internal.api.Invocations;
import dev.nthings.helm4j.internal.gateway.ChartGateway;
import dev.nthings.helm4j.internal.model.ModelSupport;
import dev.nthings.helm4j.model.ListResult;

/** Request parameters for searching configured chart repositories. */
public record RepoSearchRequest(
    String keyword,
    boolean regularExpression,
    boolean includeAllVersions,
    boolean includePreReleaseVersions,
    String versionConstraint,
    boolean failIfNoResults,
    int maxColumnWidth) {

  public RepoSearchRequest {
    keyword = ModelSupport.normalizeBlankToNull(keyword);
    versionConstraint = ModelSupport.normalizeBlankToNull(versionConstraint);
  }

  public static Builder builder() {
    return new Builder(null);
  }

  static Builder builder(ChartGateway gateway) {
    return new Builder(gateway);
  }

  public static final class Builder {
    private final ChartGateway gateway;
    private String keyword;
    private boolean regularExpression;
    private boolean includeAllVersions;
    private boolean includePreReleaseVersions;
    private String versionConstraint;
    private boolean failIfNoResults;
    private int maxColumnWidth;

    private Builder(ChartGateway gateway) {
      this.gateway = gateway;
    }

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

    /** Builds the request and runs the repository search through the bound client. */
    public ListResult<RepoChartSummary> execute() {
      return Invocations.requireBound(gateway).searchRepo(build());
    }
  }
}
