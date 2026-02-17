package dev.nthings.helm4j.release;

/** Core Helm release metadata returned by install operations. */
public record ReleaseInfo(
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
