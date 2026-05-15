package dev.nthings.helm4j.model;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import dev.nthings.helm4j.internal.model.ModelSupport;

import org.jspecify.annotations.Nullable;

/**
 * Reusable immutable list response shared by chart, release, and repository operations.
 *
 * <p>It is {@link Iterable} and exposes {@link #stream()}, so it can be consumed directly in a
 * for-each loop or a stream pipeline without unwrapping {@link #items()}.
 *
 * @param items immutable list items returned by an operation
 * @param <T> item type
 */
public record ListResult<T>(List<T> items) implements Iterable<T> {

  public ListResult(@Nullable List<T> items) {
    this.items = ModelSupport.immutableListOrEmpty(items);
  }

  public static <T> ListResult<T> of(@Nullable List<T> items) {
    return new ListResult<>(items);
  }

  public int size() {
    return items.size();
  }

  public boolean isEmpty() {
    return items.isEmpty();
  }

  public Stream<T> stream() {
    return items.stream();
  }

  @Override
  public Iterator<T> iterator() {
    return items.iterator();
  }

  public Optional<T> first() {
    return items.isEmpty() ? Optional.empty() : Optional.of(items.getFirst());
  }
}
