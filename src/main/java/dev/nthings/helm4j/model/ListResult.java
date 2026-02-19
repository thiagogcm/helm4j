package dev.nthings.helm4j.model;

import java.util.List;
import java.util.Optional;

import dev.nthings.helm4j.internal.model.ModelSupport;

/**
 * Reusable immutable list response shared by chart, release, and repository operations.
 *
 * @param items immutable list items returned by an operation
 * @param <T> item type
 */
public record ListResult<T>(List<T> items) {

  public ListResult {
    items = ModelSupport.immutableListOrEmpty(items);
  }

  public static <T> ListResult<T> of(List<T> items) {
    return new ListResult<>(items);
  }

  public int size() {
    return items.size();
  }

  public Optional<T> first() {
    return items.isEmpty() ? Optional.empty() : Optional.of(items.getFirst());
  }
}
