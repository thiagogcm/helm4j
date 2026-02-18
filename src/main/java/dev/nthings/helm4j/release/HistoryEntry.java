package dev.nthings.helm4j.release;

/** One revision in a release's history. */
public record HistoryEntry(
    int revision,
    String updated,
    String status,
    String chart,
    String chartVersion,
    String appVersion,
    String description) {}
