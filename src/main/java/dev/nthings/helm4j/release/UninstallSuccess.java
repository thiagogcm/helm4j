package dev.nthings.helm4j.release;

/** Successful uninstall completion. */
public record UninstallSuccess(ReleaseInfo release, String info) implements UninstallResult {}
