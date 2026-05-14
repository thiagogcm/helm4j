package dev.nthings.helm4j.errors;

import java.util.Objects;
import java.util.Optional;

/**
 * Uniform description of a Helm operation failure.
 *
 * <p>This is the single failure vocabulary shared by both failure channels: the sealed domain
 * results ({@code ReleaseFailure}, {@code UninstallFailure}, {@code RollbackFailure}, {@code
 * RepoAddFailure}) carry one as a value, and {@link HelmException} carries one when failure is
 * raised as an exception.
 *
 * @param message human-readable failure description; never {@code null} or blank
 * @param stage the internal stage at which the failure occurred, if known
 * @param operation the Helm operation that failed (e.g. {@code "install"}), if known
 * @param hint an optional actionable hint for resolving the failure
 */
public record HelmFailure(String message, String stage, String operation, Optional<String> hint) {

  public HelmFailure {
    message = Objects.requireNonNull(message, "message");
    hint = hint == null ? Optional.empty() : hint;
  }

  public HelmFailure(String message, String stage, String operation) {
    this(message, stage, operation, Optional.empty());
  }
}
