package dev.nthings.helm4j.chart;

import dev.nthings.helm4j.internal.api.Invocations;
import dev.nthings.helm4j.internal.gateway.ChartGateway;
import dev.nthings.helm4j.internal.model.ModelSupport;
import dev.nthings.helm4j.model.ListResult;

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
    return new Builder(null);
  }

  static Builder builder(ChartGateway gateway) {
    return new Builder(gateway);
  }

  public static final class Builder {
    private final @Nullable ChartGateway gateway;
    private @Nullable String keyword;
    private @Nullable String endpoint;
    private boolean failIfNoResults;
    private boolean listRepositoryUrl;
    private int maxColumnWidth;

    private Builder(@Nullable ChartGateway gateway) {
      this.gateway = gateway;
    }

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

    /** Builds the request and runs the hub search through the bound client. */
    public ListResult<HubChartSummary> execute() {
      return Invocations.requireBound(gateway).searchHub(build());
    }
  }
}
