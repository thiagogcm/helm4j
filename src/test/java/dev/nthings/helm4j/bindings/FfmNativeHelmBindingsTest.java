package dev.nthings.helm4j.bindings;

import java.lang.foreign.Arena;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import dev.nthings.helm4j.model.ShowMode;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FfmNativeHelmBindingsTest {

  private final Arena arena = Arena.ofAuto();

  @Test
  void searchReleasesNativeStringOnSuccess() {
    var freeCalls = new AtomicInteger();

    var bindings =
        new FfmNativeHelmBindings(
            (mode, chartRef, options) -> arena.allocateFrom("{}"),
            options -> arena.allocateFrom("{\"results\":[]}"),
            pointer -> freeCalls.incrementAndGet());

    var payload = bindings.search("{}");

    assertEquals("{\"results\":[]}", payload);
    assertEquals(1, freeCalls.get());
  }

  @Test
  void showReleasesNativeStringWhenStringReadFails() {
    var freeCalled = new AtomicBoolean();

    var bindings =
        new FfmNativeHelmBindings(
            (mode, chartRef, options) -> {
              try (var closedArena = Arena.ofConfined()) {
                return closedArena.allocateFrom("{\"mode\":\"chart\"}");
              }
            },
            options -> arena.allocateFrom("{\"results\":[]}"),
            pointer -> freeCalled.set(true));

    assertThrows(IllegalStateException.class, () -> bindings.show(ShowMode.CHART, "hello", "{}"));
    assertTrue(freeCalled.get());
  }
}
