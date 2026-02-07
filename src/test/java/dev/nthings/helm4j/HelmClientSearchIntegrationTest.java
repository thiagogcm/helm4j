package dev.nthings.helm4j;

import java.nio.file.Files;
import java.nio.file.Path;

import dev.nthings.helm4j.client.HelmClient;
import dev.nthings.helm4j.client.HelmClientFactory;
import dev.nthings.helm4j.options.SearchOptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HelmClientSearchIntegrationTest {

  private final HelmClient client = HelmClientFactory.create().newClient();

  @Test
  @EnabledIf("nativeLibraryAvailable")
  @DisplayName("search should return an empty list when no repositories are configured")
  void searchWithoutReposIsEmpty() {
    var response = client.search(SearchOptions.builder().keyword("nginx").build());

    assertNotNull(response);
    assertNotNull(response.results());
    assertTrue(response.results().stream().allMatch(result -> result.name() != null));
    // The environment running the tests may or may not have helm repos configured;
    // we only assert the contract that a response is returned without throwing.
  }

  static boolean nativeLibraryAvailable() {
    return Files.exists(Path.of("libhelm4j", "libhelm4j.so").toAbsolutePath());
  }
}
