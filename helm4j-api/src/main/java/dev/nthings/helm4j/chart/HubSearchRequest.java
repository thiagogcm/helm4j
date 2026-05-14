package dev.nthings.helm4j.chart;

import dev.nthings.helm4j.internal.model.ModelSupport;

import org.jspecify.annotations.Nullable;

/** Request parameters for searching the Helm hub endpoint. */
public record HubSearchRequest(
    @Nullable String keyword,
    @Nullable String endpoint,
    boolean failIfNoResults,
    boolean listRepositoryUrl,
    int maxColumnWidth) {

  public HubSearchRequest {
    keyword = ModelSupport.normalizeBlankToNull(keyword);
    endpoint = ModelSupport.normalizeBlankToNull(endpoint);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private @Nullable String keyword;
    private @Nullable String endpoint;
    private boolean failIfNoResults;
    private boolean listRepositoryUrl;
    private int maxColumnWidth;

    private Builder() {}

    public Builder keyword(String value) {
      this.keyword = value;
      return this;
    }

    public Builder endpoint(String value) {
      this.endpoint = value;
      return this;
    }

    public Builder failIfNoResults(boolean value) {
      this.failIfNoResults = value;
      return this;
    }

    public Builder listRepositoryUrl(boolean value) {
      this.listRepositoryUrl = value;
      return this;
    }

    public Builder maxColumnWidth(int value) {
      this.maxColumnWidth = value;
      return this;
    }

    public HubSearchRequest build() {
      return new HubSearchRequest(
          keyword, endpoint, failIfNoResults, listRepositoryUrl, maxColumnWidth);
    }
  }
}
