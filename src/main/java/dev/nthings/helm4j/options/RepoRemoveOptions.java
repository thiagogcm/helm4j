package dev.nthings.helm4j.options;

import java.util.Arrays;
import java.util.List;

/** Options accepted by {@code helm repo remove}. */
public record RepoRemoveOptions(List<String> names) {

  public RepoRemoveOptions {
    names = normalizeNames(names);
    if (names.isEmpty()) {
      throw new IllegalArgumentException("At least one repository name is required");
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  private static List<String> normalizeNames(List<String> names) {
    if (names == null) {
      return List.of();
    }
    return names.stream().map(RepoRemoveOptions::normalize).filter(value -> value != null).toList();
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

    public RepoRemoveOptions build() {
      return new RepoRemoveOptions(names);
    }
  }
}
