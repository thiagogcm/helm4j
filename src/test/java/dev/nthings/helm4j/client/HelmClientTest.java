package dev.nthings.helm4j.client;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import dev.nthings.helm4j.exceptions.HelmException;
import dev.nthings.helm4j.options.SearchOptions;
import dev.nthings.helm4j.options.ShowOptions;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HelmClientTest {

  private final Arena arena = Arena.ofAuto();

  @Test
  void showMethodsDecodePayloads() {
    var client =
        new HelmClient(
            JsonMapper.builder().build(),
            jsonShowInvoker(showPayload("chart")),
            jsonShowInvoker(showPayload("values")),
            jsonShowInvoker(showPayload("readme")),
            jsonShowInvoker(showPayload("all")),
            jsonSearchInvoker("{\"results\":[]}"),
            ptr -> {});

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
  }

  @Test
  void searchDecodesPayload() {
    var client =
        new HelmClient(
            JsonMapper.builder().build(),
            jsonShowInvoker(showPayload("all")),
            jsonShowInvoker(showPayload("all")),
            jsonShowInvoker(showPayload("all")),
            jsonShowInvoker(showPayload("all")),
            jsonSearchInvoker(
                """
                {"results":[{"name":"repo/hello","version":"1.2.3","appVersion":"2.0.0","description":"sample","score":42}]}
                """),
            ptr -> {});

    var response = client.search(SearchOptions.builder().query("hello").build());
    assertEquals(1, response.size());
    assertEquals("repo/hello", response.charts().getFirst().name());
    assertEquals("1.2.3", response.charts().getFirst().version());
  }

  @Test
  void searchStringOverloadUsesQuery() {
    var client =
        new HelmClient(
            JsonMapper.builder().build(),
            jsonShowInvoker(showPayload("all")),
            jsonShowInvoker(showPayload("all")),
            jsonShowInvoker(showPayload("all")),
            jsonShowInvoker(showPayload("all")),
            jsonSearchInvoker("{\"results\":[]}"),
            ptr -> {});

    var response = client.search("hello");
    assertTrue(response.isEmpty());
  }

  @Test
  void showErrorPayloadThrowsHelmException() {
    var client =
        new HelmClient(
            JsonMapper.builder().build(),
            jsonShowInvoker(
                """
                {"error":"boom","stage":"runShow","mode":"chart","chartRef":"hello","chartPath":"/tmp/hello"}
                """),
            jsonShowInvoker(showPayload("values")),
            jsonShowInvoker(showPayload("readme")),
            jsonShowInvoker(showPayload("all")),
            jsonSearchInvoker("{\"results\":[]}"),
            ptr -> {});

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
    var client =
        new HelmClient(
            JsonMapper.builder().build(),
            jsonShowInvoker(showPayload("chart")),
            jsonShowInvoker(showPayload("values")),
            jsonShowInvoker(showPayload("readme")),
            jsonShowInvoker(showPayload("all")),
            jsonSearchInvoker("{\"error\":\"not found\",\"stage\":\"searchRepo\"}"),
            ptr -> {});

    var ex =
        assertThrows(
            HelmException.class,
            () -> client.search(SearchOptions.builder().query("missing").build()));
    assertEquals("not found", ex.getMessage());
    assertEquals("searchRepo", ex.stage());
  }

  @Test
  void invalidNativeJsonFailsFast() {
    var badShowClient =
        new HelmClient(
            JsonMapper.builder().build(),
            jsonShowInvoker("not-json"),
            jsonShowInvoker(showPayload("values")),
            jsonShowInvoker(showPayload("readme")),
            jsonShowInvoker(showPayload("all")),
            jsonSearchInvoker("{\"results\":[]}"),
            ptr -> {});

    assertThrows(IllegalStateException.class, () -> badShowClient.showChart("hello"));

    var badSearchClient =
        new HelmClient(
            JsonMapper.builder().build(),
            jsonShowInvoker(showPayload("chart")),
            jsonShowInvoker(showPayload("values")),
            jsonShowInvoker(showPayload("readme")),
            jsonShowInvoker(showPayload("all")),
            jsonSearchInvoker("not-json"),
            ptr -> {});

    assertThrows(
        IllegalStateException.class,
        () -> badSearchClient.search(SearchOptions.builder().query("hello").build()));
  }

  @Test
  void unexpectedModeFailsFast() {
    var client =
        new HelmClient(
            JsonMapper.builder().build(),
            jsonShowInvoker(showPayload("values")),
            jsonShowInvoker(showPayload("values")),
            jsonShowInvoker(showPayload("readme")),
            jsonShowInvoker(showPayload("all")),
            jsonSearchInvoker("{\"results\":[]}"),
            ptr -> {});

    assertThrows(IllegalStateException.class, () -> client.showChart("hello"));
  }

  @Test
  void nullInputsAreRejected() {
    var client =
        new HelmClient(
            JsonMapper.builder().build(),
            jsonShowInvoker(showPayload("chart")),
            jsonShowInvoker(showPayload("values")),
            jsonShowInvoker(showPayload("readme")),
            jsonShowInvoker(showPayload("all")),
            jsonSearchInvoker("{\"results\":[]}"),
            ptr -> {});

    assertThrows(NullPointerException.class, () -> client.showChart(null));
    assertThrows(NullPointerException.class, () -> client.showAll("hello", null));
    assertThrows(NullPointerException.class, () -> client.search((SearchOptions) null));
  }

  @Test
  void nullNativePointerYieldsDecodeError() {
    var client =
        new HelmClient(
            JsonMapper.builder().build(),
            (chartRef, options) -> MemorySegment.NULL,
            jsonShowInvoker(showPayload("values")),
            jsonShowInvoker(showPayload("readme")),
            jsonShowInvoker(showPayload("all")),
            options -> MemorySegment.NULL,
            ptr -> {});

    assertThrows(IllegalStateException.class, () -> client.showChart("hello"));
    assertThrows(IllegalStateException.class, () -> client.search("hello"));
  }

  @Test
  void immutableCollectionsReturnedToConsumers() {
    var client =
        new HelmClient(
            JsonMapper.builder().build(),
            jsonShowInvoker(showPayload("chart")),
            jsonShowInvoker(showPayload("values")),
            jsonShowInvoker(showPayload("readme")),
            jsonShowInvoker(showPayload("all")),
            jsonSearchInvoker(
                "{\"results\":[{\"name\":\"x\",\"version\":\"1\",\"appVersion\":\"1\",\"description\":\"d\",\"score\":1}]}"),
            ptr -> {});

    var all = client.showAll("hello");
    assertThrows(
        UnsupportedOperationException.class, () -> all.customResourceDefinitions().add("x"));

    var search = client.search("x");
    assertFalse(search.charts().isEmpty());
    assertThrows(UnsupportedOperationException.class, () -> search.charts().clear());
  }

  private HelmClient.NativeShowInvoker jsonShowInvoker(String json) {
    return (chartRef, options) -> arena.allocateFrom(json);
  }

  private HelmClient.NativeSearchInvoker jsonSearchInvoker(String json) {
    return options -> arena.allocateFrom(json);
  }

  private static String showPayload(String mode) {
    return String.format(
        """
        {"mode":"%s","chartRef":"hello","chartPath":"/tmp/hello","sections":{"chart":"name: hello","values":"message: hello","readme":"# hello","crds":["widgets.example.com"]},"cliOutput":"ok"}
        """,
        mode);
  }
}
