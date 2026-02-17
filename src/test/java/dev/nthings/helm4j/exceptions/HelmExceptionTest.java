package dev.nthings.helm4j.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class HelmExceptionTest {

  @Test
  void exposesNativeErrorContext() {
    var exception = new HelmException("failed", "runShow", "all", "repo/hello", "/tmp/hello");

    assertEquals("failed", exception.getMessage());
    assertEquals("runShow", exception.stage());
    assertEquals("all", exception.mode());
    assertEquals("repo/hello", exception.chartRef());
    assertEquals("/tmp/hello", exception.chartPath());
    assertNull(exception.operation());
  }

  @Test
  void exposesRepoOperationContext() {
    var exception = new HelmException("failed", "runOperation", null, null, null, "repo remove");

    assertEquals("failed", exception.getMessage());
    assertEquals("runOperation", exception.stage());
    assertEquals("repo remove", exception.operation());
  }
}
