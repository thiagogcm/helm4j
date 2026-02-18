package dev.nthings.helm4j.release;

import java.util.List;

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
    namespace = normalize(namespace);
    filter = normalize(filter);
    states = states == null ? List.of() : List.copyOf(states);
    selector = normalize(selector);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private String namespace;
    private boolean allNamespaces;
    private String filter;
    private List<String> states;
    private int limit;
    private int offset;
    private boolean sortByDate;
    private boolean sortReverse;
    private String selector;

    private Builder() {}

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
  }

  private static String normalize(String value) {
    if (value == null) {
      return null;
    }
    var normalized = value.trim();
    return normalized.isEmpty() ? null : normalized;
  }
}
