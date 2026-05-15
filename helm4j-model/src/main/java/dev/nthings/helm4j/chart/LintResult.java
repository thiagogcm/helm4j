package dev.nthings.helm4j.chart;

import java.util.List;

import dev.nthings.helm4j.internal.model.ModelSupport;

/** Result of a helm lint operation. */
public record LintResult(
    List<LintMessage> messages, int totalCharts, int chartsTested, int chartsFailed) {

  public LintResult {
    messages = ModelSupport.immutableListOrEmpty(messages);
  }

  public boolean passed() {
    return chartsFailed == 0;
  }
}
