package dev.nthings.helm4j.model;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ModelMappingTest {

  @Test
  void showResponseDeserializesAndIgnoresUnknownFields() throws Exception {
    var mapper = JsonMapper.builder().build();

    var response =
        mapper.readValue(
            """
            {
              "mode":"all",
              "chartRef":"repo/hello",
              "chartPath":"/tmp/hello",
              "sections":{
                "chart":"name: hello",
                "values":"message: hello",
                "readme":"# hello",
                "crds":["widgets.example.com"]
              },
              "cliOutput":"ok",
              "unknown":"ignored"
            }
            """,
            ShowResponse.class);

    assertEquals(ShowMode.ALL, response.mode());
    assertEquals("repo/hello", response.chartRef());
    assertEquals("/tmp/hello", response.chartPath());
    assertNotNull(response.sections());
    assertEquals("name: hello", response.sections().chart());
    assertEquals(1, response.sections().crds().size());
  }

  @Test
  void searchResponseDeserializesAndIgnoresUnknownFields() throws Exception {
    var mapper = JsonMapper.builder().build();

    var response =
        mapper.readValue(
            """
            {
              "results":[
                {
                  "name":"repo/nginx",
                  "version":"1.0.0",
                  "appVersion":"2.1.0",
                  "description":"Nginx chart",
                  "score":99,
                  "unknown":"ignored"
                }
              ],
              "other":"ignored"
            }
            """,
            SearchResponse.class);

    assertEquals(1, response.results().size());
    var first = response.results().getFirst();
    assertEquals("repo/nginx", first.name());
    assertEquals("1.0.0", first.version());
    assertEquals("2.1.0", first.appVersion());
    assertEquals("Nginx chart", first.description());
    assertEquals(99, first.score());
  }
}
