package dev.nthings.helm4j.release;

/** Successful rollback completion. */
public record RollbackSuccess(String releaseName, int revision) implements RollbackResult {}
