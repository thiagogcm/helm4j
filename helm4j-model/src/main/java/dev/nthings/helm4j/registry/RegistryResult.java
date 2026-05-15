package dev.nthings.helm4j.registry;

/** Result of registry login/logout operations. */
public record RegistryResult(String hostname, String status) {}
