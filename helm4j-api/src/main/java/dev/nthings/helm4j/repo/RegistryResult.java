package dev.nthings.helm4j.repo;

/** Result of registry login/logout operations. */
public record RegistryResult(String hostname, String status) {}
