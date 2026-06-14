package dev.nthings.helm4j.errors;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * Structured description of a Helm operation failure, carried by {@link HelmCommandException}.
 *
 * @param message human-readable failure description; never {@code null} or blank
 * @param stage the internal stage at which the failure occurred, if known
 * @param operation the Helm operation that failed (e.g. {@code "install"}), if known
 */
public record HelmFailure(String message, @Nullable String stage, @Nullable String operation) {

  public HelmFailure {
    Objects.requireNonNull(message, "message");
  }
}
