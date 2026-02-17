package dev.nthings.helm4j.chart;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Structured response for hub search operations. */
public record HubSearchResult(List<HubChartSummary> charts) {

  public HubSearchResult {
    charts = List.copyOf(Objects.requireNonNullElse(charts, List.of()));
  }

  public int size() {
    return charts.size();
  }

  public Optional<HubChartSummary> first() {
    return charts.stream().findFirst();
  }
}
