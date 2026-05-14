package dev.nthings.helm4j.release;

/**
 * Outcome of an {@code install} or {@code upgrade} operation.
 *
 * <p>The two operations share this type because they share a shape: both either complete, land in a
 * pending state, or fail. A {@code switch} over this interface has exactly three real cases, so the
 * compiler-enforced exhaustiveness is meaningful.
 */
public sealed interface ReleaseResult permits ReleaseSuccess, ReleasePending, ReleaseFailure {}
