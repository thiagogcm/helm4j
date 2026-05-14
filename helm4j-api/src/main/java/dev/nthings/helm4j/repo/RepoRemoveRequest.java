package dev.nthings.helm4j.repo;

import java.util.Arrays;
import java.util.List;

import dev.nthings.helm4j.internal.api.Invocations;
import dev.nthings.helm4j.internal.gateway.RepoGateway;
import dev.nthings.helm4j.internal.model.ModelSupport;
import dev.nthings.helm4j.model.ListResult;

/** Request options for repository remove operations. */
public record RepoRemoveRequest(List<String> names) {

  public RepoRemoveRequest {
    names = normalizeNames(names);
  }

  public static Builder builder() {
    return new Builder(null);
  }

  static Builder builder(RepoGateway gateway) {
    return new Builder(gateway);
  }

  public static final class Builder {
    private final RepoGateway gateway;
    private List<String> names;

    private Builder(RepoGateway gateway) {
      this.gateway = gateway;
    }

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

    /** Builds the request and removes the repositories through the bound client. */
    public ListResult<String> execute() {
      return Invocations.requireBound(gateway).repoRemove(build());
    }
  }

  private static List<String> normalizeNames(List<String> value) {
    if (value == null || value.isEmpty()) {
      return List.of();
    }
    return value.stream().map(ModelSupport::normalizeBlankToNull).filter(v -> v != null).toList();
  }
}
