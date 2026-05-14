package dev.nthings.helm4j.chart;

import dev.nthings.helm4j.release.ReleaseInfo;

/** Result of a helm template operation. */
public record TemplateResult(ReleaseInfo release, String manifest) {}
