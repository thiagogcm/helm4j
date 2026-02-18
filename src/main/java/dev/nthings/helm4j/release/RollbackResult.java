package dev.nthings.helm4j.release;

/** Domain result hierarchy for release rollback outcomes. */
public sealed interface RollbackResult permits RollbackSuccess, RollbackFailure {
}
