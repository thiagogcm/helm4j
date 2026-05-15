package dev.nthings.helm4j;

import java.util.List;

import dev.nthings.helm4j.errors.HelmException;
import dev.nthings.helm4j.errors.HelmFailure;
import dev.nthings.helm4j.model.ListResult;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Covers the standalone value types of the public vocabulary: {@link ListResult} and errors. */
class SdkValueTypesTest {

  @Test
  void listResultIsIterableAndStreamable() {
    var empty = ListResult.<String>of(List.of());
    assertEquals(0, empty.size());
    assertTrue(empty.isEmpty());
    assertTrue(empty.first().isEmpty());

    var populated = ListResult.of(List.of("a", "b"));
    assertEquals(2, populated.size());
    assertEquals("a", populated.first().orElseThrow());
    assertEquals(2, populated.stream().count());
    var seen = 0;
    for (var ignored : populated) {
      seen++;
    }
    assertEquals(2, seen);
  }

  @Test
  void helmExceptionCarriesStructuredFailure() {
    var error = new HelmException("boom", "runOperation", "install");
    assertEquals("boom", error.getMessage());
    assertEquals("runOperation", error.stage());
    assertEquals("install", error.operation());
    assertEquals(new HelmFailure("boom", "runOperation", "install"), error.failure());

    var cause = new IllegalStateException("root");
    var wrapped = new HelmException(new HelmFailure("boom", "runOperation", "install"), cause);
    assertSame(cause, wrapped.getCause());

    assertThrows(
        HelmException.class,
        () -> {
          throw error;
        });
  }
}
