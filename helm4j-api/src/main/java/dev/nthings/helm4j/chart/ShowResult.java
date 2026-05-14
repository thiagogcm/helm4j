package dev.nthings.helm4j.chart;

import java.util.List;
import java.util.Objects;

import dev.nthings.helm4j.internal.model.ModelSupport;

/** Unified result for all {@code helm show <mode>} operations. */
public record ShowResult(
    ShowMode mode,
    String chartReference,
    String resolvedPath,
    String metadataYaml,
    String valuesYaml,
    String readmeText,
    List<String> customResourceDefinitions,
    String rawOutput) {

  public ShowResult {
    mode = Objects.requireNonNull(mode, "mode");
    customResourceDefinitions = ModelSupport.immutableListOrEmpty(customResourceDefinitions);
  }
}
