package dev.nthings.helm4j.chart;

import dev.nthings.helm4j.release.Release;

/** Result of a helm template operation. */
public record TemplateResult(Release release, String manifest) {}
