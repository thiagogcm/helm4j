package dev.nthings.helm4j;

import java.nio.file.Files;
import java.nio.file.Path;

import dev.nthings.helm4j.client.HelmClient;
import dev.nthings.helm4j.client.HelmClientFactory;
import dev.nthings.helm4j.exceptions.HelmException;
import dev.nthings.helm4j.options.SearchOptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HelmClientSearchIntegrationTest {

  private final HelmClient client = HelmClientFactory.create().newClient();

  @Test
  @EnabledIf("nativeLibraryAvailable")
  @DisplayName("search should reflect repository configuration state")
  void searchReflectsRepositoryConfigurationState() {
    try {
      var response = client.search(SearchOptions.builder().query("nginx").build());

      assertNotNull(response);
      assertNotNull(response.charts());
      assertTrue(response.charts().stream().allMatch(result -> result.name() != null));
    } catch (HelmException ex) {
      assertEquals("runOperation", ex.stage());
      assertTrue(ex.getMessage().contains("no repositories configured"));
    }
  }

  static boolean nativeLibraryAvailable() {
    return Files.exists(Path.of("libhelm4j", "libhelm4j.so").toAbsolutePath());
  }
}
