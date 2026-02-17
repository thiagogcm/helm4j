package dev.nthings.helm4j.repo;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/** Request options for repository update operations. */
public record RepoUpdateRequest(List<String> names, Duration timeout) {

  public RepoUpdateRequest {
    names = normalizeNames(names);
  }

  public static RepoUpdateRequest defaults() {
    return builder().build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private List<String> names;
    private Duration timeout;

    private Builder() {}

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
      return new RepoUpdateRequest(names, timeout);
    }
  }

  private static List<String> normalizeNames(List<String> value) {
    if (value == null || value.isEmpty()) {
      return List.of();
    }
    return value.stream().map(RepoUpdateRequest::normalize).filter(v -> v != null).toList();
  }

  private static String normalize(String value) {
    if (value == null) {
      return null;
    }
    var normalized = value.trim();
    return normalized.isEmpty() ? null : normalized;
  }
}
