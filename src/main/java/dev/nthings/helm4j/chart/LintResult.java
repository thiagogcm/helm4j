package dev.nthings.helm4j.chart;

import java.util.List;

/** Result of a helm lint operation. */
public record LintResult(
    List<LintMessage> messages, int totalCharts, int chartsTested, int chartsFailed) {

  public LintResult {
    messages = messages == null ? List.of() : List.copyOf(messages);
  }

  public boolean passed() {
    return chartsFailed == 0;
  }
}
