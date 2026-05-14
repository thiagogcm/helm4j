package dev.nthings.helm4j.errors;

/**
 * A domain result that carries a structured {@link HelmFailure}.
 *
 * <p>Implemented by the sealed {@code *Failure} result permits so callers can read the failure
 * fields directly without unwrapping {@link #failure()}.
 */
public interface HelmFailureCarrier {

  HelmFailure failure();

  default String message() {
    return failure().message();
  }

  default String stage() {
    return failure().stage();
  }

  default String operation() {
    return failure().operation();
  }
}
