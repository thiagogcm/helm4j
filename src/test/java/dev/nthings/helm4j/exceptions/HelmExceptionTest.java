package dev.nthings.helm4j.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HelmExceptionTest {

  @Test
  void exposesNativeErrorContext() {
    var exception = new HelmException("failed", "runShow", "all", "repo/hello", "/tmp/hello");

    assertEquals("failed", exception.getMessage());
    assertEquals("runShow", exception.stage());
    assertEquals("all", exception.mode());
    assertEquals("repo/hello", exception.chartRef());
    assertEquals("/tmp/hello", exception.chartPath());
  }
}
