package dev.nthings.helm4j.system;

/** Helm SDK and Go runtime version metadata. */
public record VersionInfo(String version, String goVersion, String helmVersion) {}
