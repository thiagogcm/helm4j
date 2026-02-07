package dev.nthings.helm4j.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Structured search response for {@code helm search repo}. */
public record SearchResultSet(List<ChartSummary> charts) {

  public SearchResultSet {
    charts = List.copyOf(Objects.requireNonNull(charts, "charts"));
  }

  public int size() {
    return charts.size();
  }

  public boolean isEmpty() {
    return charts.isEmpty();
  }

  public Optional<ChartSummary> first() {
    return charts.stream().findFirst();
  }
}
