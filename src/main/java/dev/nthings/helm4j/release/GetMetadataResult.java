package dev.nthings.helm4j.release;

/** Result of `helm get metadata`. */
public record GetMetadataResult(
    String name,
    String namespace,
    int revision,
    String status,
    String chart,
    String chartVersion,
    String appVersion,
    String deployedAt) {}
