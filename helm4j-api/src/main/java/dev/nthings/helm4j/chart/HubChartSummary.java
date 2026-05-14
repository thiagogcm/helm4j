package dev.nthings.helm4j.chart;

/** Single chart entry returned by hub search. */
public record HubChartSummary(
    String name,
    String version,
    String appVersion,
    String description,
    int score,
    String url,
    String repositoryName,
    String repositoryUrl) {}
