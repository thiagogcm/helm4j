package dev.nthings.helm4j.client;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import dev.nthings.helm4j.exceptions.HelmException;
import dev.nthings.helm4j.model.ShowMode;
import dev.nthings.helm4j.options.SearchOptions;
import dev.nthings.helm4j.options.ShowOptions;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    var options = ShowOptions.builder().repoUrl("https://example.com/charts").build();
    assertEquals(ShowMode.CHART, client.showChart("hello", options).mode());
    assertEquals(ShowMode.VALUES, client.showValues("hello", options).mode());
    assertEquals(ShowMode.README, client.showReadme("hello", options).mode());

    var all = client.showAll("hello", options);
    assertEquals(ShowMode.ALL, all.mode());
    assertNotNull(all.sections());
    assertEquals("name: hello", all.sections().chart());
    assertEquals("message: hello", all.sections().values());
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

    var response = client.search(SearchOptions.builder().keyword("hello").build());
    assertEquals(1, response.results().size());
    assertEquals("repo/hello", response.results().getFirst().name());
    assertEquals("1.2.3", response.results().getFirst().version());
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
                    "hello", ShowOptions.builder().repoUrl("https://example.com").build()));
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
            () -> client.search(SearchOptions.builder().keyword("missing").build()));
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

    assertThrows(
        IllegalStateException.class,
        () -> badShowClient.showChart("hello", ShowOptions.builder().build()));

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
        () -> badSearchClient.search(SearchOptions.builder().keyword("hello").build()));
  }

  @Test
  void invalidTypedPayloadFailsToDecode() {
    var client =
        new HelmClient(
            JsonMapper.builder().build(),
            jsonShowInvoker(
                """
                {"mode":"unknown","chartRef":"hello","chartPath":"/tmp/hello","sections":{},"cliOutput":"ok"}
                """),
            jsonShowInvoker(showPayload("values")),
            jsonShowInvoker(showPayload("readme")),
            jsonShowInvoker(showPayload("all")),
            jsonSearchInvoker("{\"results\":[]}"),
            ptr -> {});

    assertThrows(
        IllegalStateException.class,
        () -> client.showChart("hello", ShowOptions.builder().build()));
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

    assertThrows(
        NullPointerException.class, () -> client.showChart(null, ShowOptions.builder().build()));
    assertThrows(NullPointerException.class, () -> client.showAll("hello", null));
    assertThrows(NullPointerException.class, () -> client.search(null));
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

    assertThrows(
        IllegalStateException.class,
        () -> client.showChart("hello", ShowOptions.builder().build()));
    assertThrows(
        IllegalStateException.class,
        () -> client.search(SearchOptions.builder().keyword("hello").build()));
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
