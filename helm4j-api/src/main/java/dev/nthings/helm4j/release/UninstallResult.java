package dev.nthings.helm4j.release;

/** Outcome of an {@code uninstall} operation. */
public sealed interface UninstallResult permits UninstallSuccess, UninstallFailure {}
