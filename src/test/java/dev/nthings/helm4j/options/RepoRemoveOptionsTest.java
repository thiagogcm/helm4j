package dev.nthings.helm4j.options;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RepoRemoveOptionsTest {

  @Test
  void namesAreNormalizedAndDefensivelyCopied() {
    var names = new ArrayList<>(List.of(" bitnami ", "stable"));

    var options = RepoRemoveOptions.builder().names(names).build();
    names.clear();

    assertEquals(List.of("bitnami", "stable"), options.names());
    assertThrows(UnsupportedOperationException.class, () -> options.names().clear());
  }

  @Test
  void emptyNamesAreRejected() {
    assertThrows(IllegalArgumentException.class, () -> RepoRemoveOptions.builder().build());
    assertThrows(
        IllegalArgumentException.class,
        () -> RepoRemoveOptions.builder().names(List.of(" ", "")).build());
    assertThrows(
        IllegalArgumentException.class,
        () -> RepoRemoveOptions.builder().names((String[]) null).build());
  }
}
