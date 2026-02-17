package dev.nthings.helm4j.release;

/** Domain result hierarchy for release install outcomes. */
public sealed interface InstallResult permits InstallSuccess, InstallPending, InstallFailure {}
