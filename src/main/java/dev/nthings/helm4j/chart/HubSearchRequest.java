package dev.nthings.helm4j.chart;

/** Request parameters for searching the Helm hub endpoint. */
public record HubSearchRequest(
    String keyword,
    String endpoint,
    boolean failIfNoResults,
    boolean listRepositoryUrl,
    int maxColumnWidth) {

  public HubSearchRequest {
    keyword = normalize(keyword);
    endpoint = normalize(endpoint);
  }

  public static HubSearchRequest defaults(String keyword) {
    return builder().keyword(keyword).build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private String keyword;
    private String endpoint;
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

  private static String normalize(String value) {
    if (value == null) {
      return null;
    }
    var normalized = value.trim();
    return normalized.isEmpty() ? null : normalized;
  }
}
