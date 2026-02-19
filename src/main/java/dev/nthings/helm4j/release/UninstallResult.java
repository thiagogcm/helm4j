package dev.nthings.helm4j.release;

/** Domain result hierarchy for release uninstall outcomes. */
public sealed interface UninstallResult permits UninstallSuccess, ReleaseFailure {}
