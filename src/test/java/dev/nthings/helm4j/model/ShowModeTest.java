package dev.nthings.helm4j.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ShowModeTest {

  @Test
  void fromStringAcceptsMixedCase() {
    assertEquals(ShowMode.CHART, ShowMode.fromString("chart"));
    assertEquals(ShowMode.VALUES, ShowMode.fromString("VaLuEs"));
    assertEquals(ShowMode.README, ShowMode.fromString("README"));
    assertEquals(ShowMode.ALL, ShowMode.fromString("all"));
    assertEquals(ShowMode.CRDS, ShowMode.fromString("crds"));
  }

  @Test
  void fromStringReturnsNullForBlankOrNull() {
    assertNull(ShowMode.fromString(null));
    assertNull(ShowMode.fromString(" "));
  }

  @Test
  void toJsonUsesLowercaseName() {
    assertEquals("chart", ShowMode.CHART.toJson());
    assertEquals("values", ShowMode.VALUES.toJson());
    assertEquals("readme", ShowMode.README.toJson());
    assertEquals("all", ShowMode.ALL.toJson());
    assertEquals("crds", ShowMode.CRDS.toJson());
  }
}
