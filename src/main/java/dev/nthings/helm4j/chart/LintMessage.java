package dev.nthings.helm4j.chart;

/** A single lint finding. */
public record LintMessage(LintSeverity severity, String message) {
}
