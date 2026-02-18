package dev.nthings.helm4j.chart;

import java.util.List;
import java.util.Optional;

import dev.nthings.helm4j.internal.model.ModelSupport;

/** Structured response for hub search operations. */
public record HubSearchResult(List<HubChartSummary> charts) {

  public HubSearchResult {
    charts = ModelSupport.immutableListOrEmpty(charts);
  }

  public int size() {
    return charts.size();
  }

  public Optional<HubChartSummary> first() {
    return charts.isEmpty() ? Optional.empty() : Optional.of(charts.getFirst());
  }
}
