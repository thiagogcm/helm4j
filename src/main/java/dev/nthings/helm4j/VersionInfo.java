package dev.nthings.helm4j;

/** Helm SDK and Go runtime version metadata. */
public record VersionInfo(String version, String goVersion, String helmVersion) {}
