package dev.nthings.helm4j;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import dev.nthings.helm4j.errors.HelmException;

import org.jspecify.annotations.Nullable;

/**
 * No-throw capture helper for a Helm operation result.
 *
 * <p>The default API throws {@link HelmException} on failure. When that is inconvenient — bulk
 * scripted workflows, fan-out over many releases — wrap the call in {@link #capture(Supplier)} and
 * inspect the result without try/catch:
 *
 * <pre>{@code
 * HelmResult<Release> result = HelmResult.capture(() -> helm.releases().install(request));
 * if (result.isOk()) {
 *   Release release = result.value();
 * } else {
 *   HelmException failure = result.error();
 * }
 * }</pre>
 *
 * @param <T> the success value type
 */
public final class HelmResult<T> {

  private final @Nullable T value;
  private final @Nullable HelmException error;

  private HelmResult(@Nullable T value, @Nullable HelmException error) {
    this.value = value;
    this.error = error;
  }

  public static <T> HelmResult<T> ok(T value) {
    return new HelmResult<>(Objects.requireNonNull(value, "value"), null);
  }

  public static <T> HelmResult<T> error(HelmException error) {
    return new HelmResult<>(null, Objects.requireNonNull(error, "error"));
  }

  /**
   * Runs {@code action} and wraps the outcome. Any {@link HelmException} becomes an {@link
   * #error()} result; any other throwable propagates.
   */
  public static <T> HelmResult<T> capture(Supplier<T> action) {
    Objects.requireNonNull(action, "action");
    try {
      return ok(action.get());
    } catch (HelmException e) {
      return error(e);
    }
  }

  public boolean isOk() {
    return error == null;
  }

  public boolean isError() {
    return error != null;
  }

  /** The success value. Use {@link #isOk()} first. */
  public T value() {
    if (value == null) {
      throw new IllegalStateException("result is an error", error);
    }
    return value;
  }

  /** The captured failure. Use {@link #isError()} first. */
  public HelmException error() {
    if (error == null) {
      throw new IllegalStateException("result is a success");
    }
    return error;
  }

  public Optional<T> asOptional() {
    return Optional.ofNullable(value);
  }

  /** Re-throws if this is an error result; otherwise returns the success value. */
  public T orThrow() {
    if (error != null) {
      throw error;
    }
    return value;
  }
}
