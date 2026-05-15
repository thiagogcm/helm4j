package dev.nthings.helm4j.runtime.ffm.internal;

/** JSON-bridge representation of a Helm release, shared across the release-oriented gateways. */
record NativeReleasePayload(
    String name,
    String namespace,
    int revision,
    String status,
    String description,
    String firstDeployed,
    String lastDeployed,
    String chartName,
    String chartVersion,
    String appVersion,
    String notes) {}
