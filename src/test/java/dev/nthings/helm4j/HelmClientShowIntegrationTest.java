package dev.nthings.helm4j;

import java.nio.file.Path;

import dev.nthings.helm4j.client.HelmClient;
import dev.nthings.helm4j.client.HelmClientFactory;
import dev.nthings.helm4j.exceptions.HelmException;
import dev.nthings.helm4j.model.ShowMode;
import dev.nthings.helm4j.options.ShowOptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HelmClientShowIntegrationTest {

  private final HelmClient client = HelmClientFactory.create().newClient();

  @Test
  @DisplayName("showAll should return all sections for a local chart")
  void showAllLocalChart() {
    var chartPath = Path.of("src", "test", "resources", "charts", "hello").toAbsolutePath();

    var response = client.showAll(chartPath.toString(), ShowOptions.builder().build());

    assertEquals(ShowMode.ALL, response.mode());
    assertEquals(chartPath.toString(), response.chartRef());
    assertNotNull(response.sections());
    assertTrue(response.sections().chart().contains("name: hello"));
    assertTrue(response.sections().values().contains("message: Hello helm4j"));
    assertTrue(response.sections().readme().contains("Hello Chart"));
    assertFalse(response.sections().crds().isEmpty());
    assertNotNull(response.cliOutput());
    assertTrue(response.cliOutput().contains("hello"));
  }

  @Test
  @DisplayName("blank chart reference should raise HelmException")
  void showWithBlankRefFails() {
    var ex =
        assertThrows(
            HelmException.class, () -> client.showChart("  ", ShowOptions.builder().build()));

    assertEquals("runShow", ex.stage());
    assertEquals("chart", ex.mode());
  }
}
