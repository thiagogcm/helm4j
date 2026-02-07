package dev.nthings.helm4j.client;

import java.util.EnumMap;
import java.util.Map;

import dev.nthings.helm4j.bindings.NativeHelmBindings;
import dev.nthings.helm4j.exceptions.HelmException;
import dev.nthings.helm4j.model.ShowMode;
import dev.nthings.helm4j.options.SearchOptions;
import dev.nthings.helm4j.options.ShowOptions;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HelmClientTest {

  @Test
  void showMethodsDecodePayloads() {
    var bindings = new StubBindings();
    bindings.putShowResponse(ShowMode.CHART, showPayload("chart"));
    bindings.putShowResponse(ShowMode.VALUES, showPayload("values"));
    bindings.putShowResponse(ShowMode.README, showPayload("readme"));
    bindings.putShowResponse(ShowMode.ALL, showPayload("all"));
    bindings.putShowResponse(ShowMode.CRDS, showPayload("crds"));

    var client = new HelmClient(JsonMapper.builder().build(), bindings);
    var options = ShowOptions.builder().repositoryUrl("https://example.com/charts").build();

    var chart = client.showChart("hello", options);
    assertEquals("hello", chart.chartReference());
    assertEquals("name: hello", chart.metadataYaml());

    var values = client.showValues("hello", options);
    assertEquals("hello", values.chartReference());
    assertEquals("message: hello", values.valuesYaml());

    var readme = client.showReadme("hello", options);
    assertEquals("hello", readme.chartReference());
    assertEquals("# hello", readme.readmeText());

    var all = client.showAll("hello", options);
    assertEquals("name: hello", all.metadataYaml());
    assertEquals("message: hello", all.valuesYaml());
    assertEquals("# hello", all.readmeText());
    assertEquals(1, all.customResourceDefinitions().size());

    var crds = client.showCrds("hello", options);
    assertEquals(1, crds.customResourceDefinitions().size());
    assertEquals("widgets.example.com", crds.customResourceDefinitions().getFirst());
  }

  @Test
  void searchDecodesPayload() {
    var bindings = new StubBindings();
    bindings.putShowResponse(ShowMode.ALL, showPayload("all"));
    bindings.setSearchResponse(
        """
        {"results":[{"name":"repo/hello","version":"1.2.3","appVersion":"2.0.0","description":"sample","score":42}]}
        """);

    var client = new HelmClient(JsonMapper.builder().build(), bindings);

    var response = client.search(SearchOptions.builder().query("hello").build());
    assertEquals(1, response.size());
    assertEquals("repo/hello", response.charts().getFirst().name());
    assertEquals("1.2.3", response.charts().getFirst().version());
  }

  @Test
  void searchStringOverloadUsesQuery() {
    var bindings = new StubBindings();
    bindings.setSearchResponse("{\"results\":[]}");
    var client = new HelmClient(JsonMapper.builder().build(), bindings);

    var response = client.search("hello");
    assertTrue(response.isEmpty());
    assertTrue(bindings.lastSearchOptionsJson().contains("\"keyword\":\"hello\""));
  }

  @Test
  void showErrorPayloadThrowsHelmException() {
    var bindings = new StubBindings();
    bindings.putShowResponse(
        ShowMode.CHART,
        """
        {"error":"boom","stage":"runShow","mode":"chart","chartRef":"hello","chartPath":"/tmp/hello"}
        """);

    var client = new HelmClient(JsonMapper.builder().build(), bindings);

    var ex =
        assertThrows(
            HelmException.class,
            () ->
                client.showChart(
                    "hello", ShowOptions.builder().repositoryUrl("https://example.com").build()));
    assertEquals("boom", ex.getMessage());
    assertEquals("runShow", ex.stage());
    assertEquals("chart", ex.mode());
    assertEquals("hello", ex.chartRef());
  }

  @Test
  void searchErrorPayloadThrowsHelmException() {
    var bindings = new StubBindings();
    bindings.setSearchResponse("{\"error\":\"not found\",\"stage\":\"searchRepo\"}");

    var client = new HelmClient(JsonMapper.builder().build(), bindings);

    var ex =
        assertThrows(
            HelmException.class,
            () -> client.search(SearchOptions.builder().query("missing").build()));
    assertEquals("not found", ex.getMessage());
    assertEquals("searchRepo", ex.stage());
  }

  @Test
  void invalidNativeJsonFailsFast() {
    var badShowBindings = new StubBindings();
    badShowBindings.putShowResponse(ShowMode.CHART, "not-json");

    var badShowClient = new HelmClient(JsonMapper.builder().build(), badShowBindings);
    assertThrows(IllegalStateException.class, () -> badShowClient.showChart("hello"));

    var badSearchBindings = new StubBindings();
    badSearchBindings.setSearchResponse("not-json");

    var badSearchClient = new HelmClient(JsonMapper.builder().build(), badSearchBindings);

    assertThrows(
        IllegalStateException.class,
        () -> badSearchClient.search(SearchOptions.builder().query("hello").build()));
  }

  @Test
  void unexpectedModeFailsFast() {
    var bindings = new StubBindings();
    bindings.putShowResponse(ShowMode.CHART, showPayload("values"));

    var client = new HelmClient(JsonMapper.builder().build(), bindings);

    assertThrows(IllegalStateException.class, () -> client.showChart("hello"));
  }

  @Test
  void nullInputsAreRejected() {
    var bindings = new StubBindings();
    bindings.putShowResponse(ShowMode.CHART, showPayload("chart"));
    bindings.setSearchResponse("{\"results\":[]}");

    var client = new HelmClient(JsonMapper.builder().build(), bindings);

    assertThrows(NullPointerException.class, () -> client.showChart(null));
    assertThrows(NullPointerException.class, () -> client.showAll("hello", null));
    assertThrows(NullPointerException.class, () -> client.search((SearchOptions) null));
  }

  @Test
  void emptyNativePayloadFailsFast() {
    var bindings = new StubBindings();
    bindings.putShowResponse(ShowMode.CHART, " ");
    bindings.setSearchResponse("");

    var client = new HelmClient(JsonMapper.builder().build(), bindings);

    assertThrows(IllegalStateException.class, () -> client.showChart("hello"));
    assertThrows(IllegalStateException.class, () -> client.search("hello"));
  }

  @Test
  void immutableCollectionsReturnedToConsumers() {
    var bindings = new StubBindings();
    bindings.putShowResponse(ShowMode.ALL, showPayload("all"));
    bindings.setSearchResponse(
        "{\"results\":[{\"name\":\"x\",\"version\":\"1\",\"appVersion\":\"1\",\"description\":\"d\",\"score\":1}]}");

    var client = new HelmClient(JsonMapper.builder().build(), bindings);

    var all = client.showAll("hello");
    assertThrows(
        UnsupportedOperationException.class, () -> all.customResourceDefinitions().add("x"));

    var search = client.search("x");
    assertFalse(search.charts().isEmpty());
    assertThrows(UnsupportedOperationException.class, () -> search.charts().clear());
  }

  private static String showPayload(String mode) {
    return String.format(
        """
        {"mode":"%s","chartRef":"hello","chartPath":"/tmp/hello","sections":{"chart":"name: hello","values":"message: hello","readme":"# hello","crds":["widgets.example.com"]},"cliOutput":"ok"}
        """,
        mode);
  }

  private static final class StubBindings implements NativeHelmBindings {
    private final Map<ShowMode, String> showResponses = new EnumMap<>(ShowMode.class);
    private String searchResponse = "{\"results\":[]}";
    private String lastSearchOptionsJson;

    @Override
    public String show(ShowMode mode, String chartReference, String optionsJson) {
      return showResponses.getOrDefault(mode, showPayload(mode.toJson()));
    }

    @Override
    public String search(String optionsJson) {
      lastSearchOptionsJson = optionsJson;
      return searchResponse;
    }

    void putShowResponse(ShowMode mode, String payload) {
      showResponses.put(mode, payload);
    }

    void setSearchResponse(String payload) {
      searchResponse = payload;
    }

    String lastSearchOptionsJson() {
      return lastSearchOptionsJson;
    }
  }
}
