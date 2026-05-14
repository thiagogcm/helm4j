package dev.nthings.helm4j.errors;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * Exception surfaced when a Helm operation fails through the exception channel.
 *
 * <p>Inspection and read operations ({@code status}, {@code list}, {@code get*}, {@code search*},
 * {@code show}, {@code template}, {@code lint}, repository updates, registry login/logout) raise
 * this exception on failure. Lifecycle mutations ({@code install}, {@code upgrade}, {@code
 * uninstall}, {@code rollback}, {@code repo add}) instead return a sealed result with an explicit
 * failure permit, since a failed mutation is expected domain data rather than an exceptional event.
 *
 * <p>Either way the failure is described by a {@link HelmFailure}, so a failure can move between
 * the value channel and the exception channel without losing information.
 */
public final class HelmException extends RuntimeException {

  private final HelmFailure failure;

  public HelmException(HelmFailure failure) {
    this(failure, null);
  }

  public HelmException(HelmFailure failure, @Nullable Throwable cause) {
    super(Objects.requireNonNull(failure, "failure").message(), cause);
    this.failure = failure;
  }

  public HelmException(String message, @Nullable String stage, @Nullable String operation) {
    this(new HelmFailure(message, stage, operation), null);
  }

  public HelmException(
      String message, @Nullable String stage, @Nullable String operation, Throwable cause) {
    this(new HelmFailure(message, stage, operation), cause);
  }

  /** The structured description of this failure. */
  public HelmFailure failure() {
    return failure;
  }

  public @Nullable String stage() {
    return failure.stage();
  }

  public @Nullable String operation() {
    return failure.operation();
  }
}
