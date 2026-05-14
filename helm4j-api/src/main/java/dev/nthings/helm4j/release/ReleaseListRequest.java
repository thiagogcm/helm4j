package dev.nthings.helm4j.release;

import java.util.List;

import dev.nthings.helm4j.internal.api.Invocations;
import dev.nthings.helm4j.internal.gateway.ReleaseGateway;
import dev.nthings.helm4j.internal.model.ModelSupport;
import dev.nthings.helm4j.model.ListResult;

/** Request parameters for listing releases. */
public record ReleaseListRequest(
    String namespace,
    boolean allNamespaces,
    String filter,
    List<String> states,
    int limit,
    int offset,
    boolean sortByDate,
    boolean sortReverse,
    String selector) {

  public ReleaseListRequest {
    namespace = ModelSupport.normalizeBlankToNull(namespace);
    filter = ModelSupport.normalizeBlankToNull(filter);
    states = ModelSupport.immutableListOrEmpty(states);
    selector = ModelSupport.normalizeBlankToNull(selector);
  }

  public static Builder builder() {
    return new Builder(null);
  }

  static Builder builder(ReleaseGateway gateway) {
    return new Builder(gateway);
  }

  public static final class Builder {
    private final ReleaseGateway gateway;
    private String namespace;
    private boolean allNamespaces;
    private String filter;
    private List<String> states;
    private int limit;
    private int offset;
    private boolean sortByDate;
    private boolean sortReverse;
    private String selector;

    private Builder(ReleaseGateway gateway) {
      this.gateway = gateway;
    }

    public Builder namespace(String value) {
      this.namespace = value;
      return this;
    }

    public Builder allNamespaces(boolean value) {
      this.allNamespaces = value;
      return this;
    }

    public Builder filter(String value) {
      this.filter = value;
      return this;
    }

    public Builder states(List<String> value) {
      this.states = value;
      return this;
    }

    public Builder limit(int value) {
      this.limit = value;
      return this;
    }

    public Builder offset(int value) {
      this.offset = value;
      return this;
    }

    public Builder sortByDate(boolean value) {
      this.sortByDate = value;
      return this;
    }

    public Builder sortReverse(boolean value) {
      this.sortReverse = value;
      return this;
    }

    public Builder selector(String value) {
      this.selector = value;
      return this;
    }

    public ReleaseListRequest build() {
      return new ReleaseListRequest(
          namespace,
          allNamespaces,
          filter,
          states,
          limit,
          offset,
          sortByDate,
          sortReverse,
          selector);
    }

    /** Builds the request and lists releases through the bound client. */
    public ListResult<ReleaseInfo> execute() {
      return Invocations.requireBound(gateway).list(build());
    }
  }
}
