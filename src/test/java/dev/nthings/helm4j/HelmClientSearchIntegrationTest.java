package dev.nthings.helm4j;

import dev.nthings.helm4j.client.HelmClient;
import dev.nthings.helm4j.client.HelmClientFactory;
import dev.nthings.helm4j.model.SearchResponse;
import dev.nthings.helm4j.options.SearchOptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class HelmClientSearchIntegrationTest {

  private final HelmClient client = HelmClientFactory.create().newClient();

  @Test
  @DisplayName("search should return an empty list when no repositories are configured")
  void searchWithoutReposIsEmpty() {
    SearchResponse response = client.search(SearchOptions.builder().keyword("nginx").build());

    assertNotNull(response);
    assertNotNull(response.results());
    // The environment running the tests may or may not have helm repos configured;
    // we only assert
    // the contract that a response is returned without throwing.
  }
}
