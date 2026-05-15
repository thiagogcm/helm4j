package dev.nthings.helm4j.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dev.nthings.helm4j.internal.model.ModelSupport;

import org.jspecify.annotations.Nullable;

/** Request options for repository remove operations. */
public record RemoveRepository(List<String> names) {

  public RemoveRepository {
    names = normalizeNames(names);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private @Nullable List<String> names;

    private Builder() {}

    public Builder names(List<String> value) {
      this.names = value;
      return this;
    }

    public Builder names(String... value) {
      this.names = value == null ? null : Arrays.asList(value);
      return this;
    }

    public RemoveRepository build() {
      return new RemoveRepository(normalizeNames(names));
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
