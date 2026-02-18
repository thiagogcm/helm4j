package dev.nthings.helm4j.chart;

import java.util.List;
import java.util.Optional;

import dev.nthings.helm4j.internal.model.ModelSupport;

/** Structured result for repository search operations. */
public record RepoSearchResult(List<RepoChartSummary> charts) {

  public RepoSearchResult {
    charts = ModelSupport.immutableListOrEmpty(charts);
  }

  public int size() {
    return charts.size();
  }

  public Optional<RepoChartSummary> first() {
    return charts.isEmpty() ? Optional.empty() : Optional.of(charts.getFirst());
  }
}
