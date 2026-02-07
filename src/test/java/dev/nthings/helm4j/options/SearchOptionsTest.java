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
            .query("grafana")
            .regularExpression(true)
            .includeAllVersions(true)
            .includePreReleaseVersions(true)
            .versionConstraint(">=1.0.0")
            .failIfNoResults(true)
            .build();

    assertEquals("grafana", options.query());
    assertTrue(options.regularExpression());
    assertTrue(options.includeAllVersions());
    assertTrue(options.includePreReleaseVersions());
    assertEquals(">=1.0.0", options.versionConstraint());
    assertTrue(options.failIfNoResults());
  }

  @Test
  void defaultsUseDeterministicBooleansAndNullStrings() {
    var options = SearchOptions.defaults();

    assertNull(options.query());
    assertFalse(options.regularExpression());
    assertFalse(options.includeAllVersions());
    assertFalse(options.includePreReleaseVersions());
    assertNull(options.versionConstraint());
    assertFalse(options.failIfNoResults());
  }

  @Test
  void builderCanSetFalseFlags() {
    var options =
        SearchOptions.builder()
            .regularExpression(false)
            .includeAllVersions(false)
            .includePreReleaseVersions(false)
            .failIfNoResults(false)
            .build();

    assertFalse(options.regularExpression());
    assertFalse(options.includeAllVersions());
    assertFalse(options.includePreReleaseVersions());
    assertFalse(options.failIfNoResults());
  }

  @Test
  void blankStringsAreNormalizedToNull() {
    var options = SearchOptions.builder().query("  ").versionConstraint(" ").build();

    assertNull(options.query());
    assertNull(options.versionConstraint());
  }
}
