package dev.nthings.helm4j.chart;

/** Single chart entry returned by repository search. */
public record RepoChartSummary(
    String name,
    String version,
    String appVersion,
    String description,
    int score,
    String repositoryName,
    String repositoryUrl) {}
