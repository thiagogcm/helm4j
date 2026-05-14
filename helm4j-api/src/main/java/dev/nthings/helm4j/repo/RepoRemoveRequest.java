package dev.nthings.helm4j.repo;

import java.util.Arrays;
import java.util.List;

import dev.nthings.helm4j.internal.model.ModelSupport;

/** Request options for repository remove operations. */
public record RepoRemoveRequest(List<String> names) {

  public RepoRemoveRequest {
    names = normalizeNames(names);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private List<String> names;

    private Builder() {}

    public Builder names(List<String> value) {
      this.names = value;
      return this;
    }

    public Builder names(String... value) {
      this.names = value == null ? null : Arrays.asList(value);
      return this;
    }

    public RepoRemoveRequest build() {
      return new RepoRemoveRequest(names);
    }
  }

  private static List<String> normalizeNames(List<String> value) {
    if (value == null || value.isEmpty()) {
      return List.of();
    }
    return value.stream().map(ModelSupport::normalizeBlankToNull).filter(v -> v != null).toList();
  }
}
