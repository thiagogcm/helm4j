package dev.nthings.helm4j.options;

import java.util.Arrays;
import java.util.List;

/** Options accepted by {@code helm repo update}. */
public record RepoUpdateOptions(List<String> names) {

  public RepoUpdateOptions {
    names = normalizeNames(names);
  }

  public static RepoUpdateOptions defaults() {
    return builder().build();
  }

  public static Builder builder() {
    return new Builder();
  }

  private static List<String> normalizeNames(List<String> names) {
    if (names == null) {
      return List.of();
    }
    return names.stream().map(RepoUpdateOptions::normalize).filter(value -> value != null).toList();
  }

  private static String normalize(String value) {
    if (value == null) {
      return null;
    }
    var trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  public static final class Builder {
    private List<String> names;

    private Builder() {}

    public Builder names(List<String> names) {
      this.names = names;
      return this;
    }

    public Builder names(String... names) {
      this.names = names == null ? null : Arrays.asList(names);
      return this;
    }

    public RepoUpdateOptions build() {
      return new RepoUpdateOptions(names);
    }
  }
}
