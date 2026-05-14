package dev.nthings.helm4j.release;

/** Outcome of a {@code rollback} operation. */
public sealed interface RollbackResult permits RollbackSuccess, RollbackFailure {}
