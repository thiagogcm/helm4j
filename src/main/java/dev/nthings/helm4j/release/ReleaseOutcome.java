package dev.nthings.helm4j.release;

/** Unified outcome hierarchy for release lifecycle operations. */
public sealed interface ReleaseOutcome
    permits ReleaseSuccess, ReleasePending, ReleaseFailure, UninstallSuccess, RollbackSuccess {}
