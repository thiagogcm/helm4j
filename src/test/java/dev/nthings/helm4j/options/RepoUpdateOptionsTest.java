package dev.nthings.helm4j.options;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepoUpdateOptionsTest {

  @Test
  void defaultsUpdateAll() {
    var options = RepoUpdateOptions.defaults();
    assertTrue(options.names().isEmpty());
  }

  @Test
  void namesAreNormalizedAndDefensivelyCopied() {
    var names = new ArrayList<>(List.of(" bitnami ", "", "stable"));

    var options = RepoUpdateOptions.builder().names(names).build();
    names.clear();

    assertEquals(List.of("bitnami", "stable"), options.names());
    assertThrows(UnsupportedOperationException.class, () -> options.names().clear());
  }

  @Test
  void varargsNullFallsBackToUpdateAll() {
    var options = RepoUpdateOptions.builder().names((String[]) null).build();
    assertTrue(options.names().isEmpty());
  }
}
