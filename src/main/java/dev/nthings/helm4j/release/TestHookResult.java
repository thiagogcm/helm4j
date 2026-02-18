package dev.nthings.helm4j.release;

/** Result of a single release test hook. */
public record TestHookResult(String name, String status) {}
