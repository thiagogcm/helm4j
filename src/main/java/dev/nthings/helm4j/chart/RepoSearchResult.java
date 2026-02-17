package dev.nthings.helm4j.chart;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Structured result for repository search operations. */
public record RepoSearchResult(List<RepoChartSummary> charts) {

  public RepoSearchResult {
    charts = List.copyOf(Objects.requireNonNullElse(charts, List.of()));
  }

  public int size() {
    return charts.size();
  }

  public Optional<RepoChartSummary> first() {
    return charts.stream().findFirst();
  }
}
