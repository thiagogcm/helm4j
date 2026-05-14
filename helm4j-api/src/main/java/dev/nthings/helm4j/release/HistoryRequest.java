package dev.nthings.helm4j.release;

import dev.nthings.helm4j.internal.api.Invocations;
import dev.nthings.helm4j.internal.gateway.ReleaseGateway;
import dev.nthings.helm4j.internal.model.ModelSupport;
import dev.nthings.helm4j.model.ListResult;

import org.jspecify.annotations.Nullable;

/** Request parameters for viewing release history. */
public record HistoryRequest(@Nullable String releaseName, @Nullable String namespace, int max) {

  public HistoryRequest {
    releaseName = ModelSupport.normalizeBlankToNull(releaseName);
    namespace = ModelSupport.normalizeBlankToNull(namespace);
  }

  public static Builder builder() {
    return new Builder(null);
  }

  static Builder builder(ReleaseGateway gateway) {
    return new Builder(gateway);
  }

  public static final class Builder {
    private final ReleaseGateway gateway;
    private String releaseName;
    private String namespace;
    private int max;

    private Builder(ReleaseGateway gateway) {
      this.gateway = gateway;
    }

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

    /** Builds the request and fetches history through the bound client. */
    public ListResult<HistoryEntry> execute() {
      return Invocations.requireBound(gateway).history(build());
    }
  }
}
