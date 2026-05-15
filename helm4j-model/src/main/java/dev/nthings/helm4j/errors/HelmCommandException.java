package dev.nthings.helm4j.errors;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * A Helm command reached the runtime and failed there.
 *
 * <p>The structured cause is the {@link HelmFailure} returned by the runtime, accessible via {@link
 * #failure()}. The message of this exception is {@link HelmFailure#message() failure.message}.
 */
public final class HelmCommandException extends HelmException {

  private final HelmFailure failure;

  public HelmCommandException(HelmFailure failure) {
    this(failure, null);
  }

  public HelmCommandException(HelmFailure failure, @Nullable Throwable cause) {
    super(Objects.requireNonNull(failure, "failure").message(), cause);
    this.failure = failure;
  }

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
