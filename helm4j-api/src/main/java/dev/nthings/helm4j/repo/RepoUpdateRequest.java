package dev.nthings.helm4j.repo;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dev.nthings.helm4j.internal.api.Invocations;
import dev.nthings.helm4j.internal.gateway.RepoGateway;
import dev.nthings.helm4j.internal.model.ModelSupport;
import dev.nthings.helm4j.model.ListResult;

import org.jspecify.annotations.Nullable;

/** Request options for repository update operations. */
public record RepoUpdateRequest(List<String> names, @Nullable Duration timeout) {

  public RepoUpdateRequest {
    names = normalizeNames(names);
  }

  public static Builder builder() {
    return new Builder(null);
  }

  static Builder builder(RepoGateway gateway) {
    return new Builder(gateway);
  }

  public static final class Builder {
    private final @Nullable RepoGateway gateway;
    private @Nullable List<String> names;
    private @Nullable Duration timeout;

    private Builder(@Nullable RepoGateway gateway) {
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

    public Builder timeout(Duration value) {
      this.timeout = value;
      return this;
    }

    public RepoUpdateRequest build() {
      return new RepoUpdateRequest(normalizeNames(names), timeout);
    }

    /** Builds the request and updates the repositories through the bound client. */
    public ListResult<RepoUpdateEntry> execute() {
      return Invocations.requireBound(gateway).repoUpdate(build());
    }
  }

  private static List<String> normalizeNames(@Nullable List<String> value) {
    if (value == null || value.isEmpty()) {
      return List.of();
    }
    var normalized = new ArrayList<String>();
    for (var name : value) {
      var trimmed = ModelSupport.normalizeBlankToNull(name);
      if (trimmed != null) {
        normalized.add(trimmed);
      }
    }
    return List.copyOf(normalized);
  }
}
