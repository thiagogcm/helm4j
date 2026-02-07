package dev.nthings.helm4j.options;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchOptionsTest {

  @Test
  void builderSetsAllFields() {
    var options =
        SearchOptions.builder()
            .keyword("grafana")
            .regexp(true)
            .versions(true)
            .devel(true)
            .version(">=1.0.0")
            .failOnNoResult(true)
            .build();

    assertEquals("grafana", options.keyword());
    assertTrue(options.regexp());
    assertTrue(options.versions());
    assertTrue(options.devel());
    assertEquals(">=1.0.0", options.version());
    assertTrue(options.failOnNoResult());
  }

  @Test
  void builderDefaultsToNullValues() {
    var options = SearchOptions.builder().build();

    assertNull(options.keyword());
    assertNull(options.regexp());
    assertNull(options.versions());
    assertNull(options.devel());
    assertNull(options.version());
    assertNull(options.failOnNoResult());
  }

  @Test
  void builderCanSetFalseFlags() {
    var options =
        SearchOptions.builder()
            .regexp(false)
            .versions(false)
            .devel(false)
            .failOnNoResult(false)
            .build();

    assertFalse(options.regexp());
    assertFalse(options.versions());
    assertFalse(options.devel());
    assertFalse(options.failOnNoResult());
  }
}
