package dev.nthings.helm4j;

import dev.nthings.helm4j.client.HelmClientFactory;
import dev.nthings.helm4j.options.SearchOptions;
import dev.nthings.helm4j.options.ShowOptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class App {
  private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

  private static final String SEARCH_KEYWORD = "grafana";
  private static final String CHART_REF = "grafana";
  private static final String REPO_URL = "https://grafana.github.io/helm-charts";

  public static void main(String[] args) {
    var client = HelmClientFactory.create().newClient();

    // Search for charts
    LOGGER.info("Searching for charts with keyword: {}", SEARCH_KEYWORD);
    var searchResponse = client.search(SearchOptions.builder().keyword(SEARCH_KEYWORD).build());

    LOGGER.info("Found {} charts:", searchResponse.results().size());
    searchResponse
        .results()
        .forEach(
            result ->
                LOGGER.info(
                    "  - {} (v{}) - {}", result.name(), result.version(), result.description()));

    // Show all details for a remote chart
    LOGGER.info("Fetching chart details for: {}", CHART_REF);
    var showResponse = client.showAll(CHART_REF, ShowOptions.builder().repoUrl(REPO_URL).build());

    LOGGER.info("Chart: {}", showResponse.chartRef());
    LOGGER.info("Resolved path: {}", showResponse.chartPath());

    var chartSection = showResponse.sections().chart();
    if (chartSection != null && !chartSection.isBlank()) {
      LOGGER.info("Chart metadata:\n{}", chartSection);
    }

    var valuesSection = showResponse.sections().values();
    if (valuesSection != null && !valuesSection.isBlank()) {
      LOGGER.info(
          "Values preview (first 500 chars):\n{}",
          valuesSection.substring(0, Math.min(500, valuesSection.length())));
    }
  }
}
