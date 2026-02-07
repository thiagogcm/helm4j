package dev.nthings.helm4j.model;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModelMappingTest {

  @Test
  void chartDetailsDefensivelyCopiesCrds() {
    var crds = new ArrayList<String>();
    crds.add("widgets.example.com");

    var details =
        new ChartDetails("repo/hello", "/tmp/hello", "meta", "values", "readme", crds, "raw");

    crds.add("gadgets.example.com");
    assertEquals(1, details.customResourceDefinitions().size());
    assertThrows(
        UnsupportedOperationException.class,
        () -> details.customResourceDefinitions().add("new.example.com"));
  }

  @Test
  void chartCrdsDefensivelyCopiesCrds() {
    var crds = new ArrayList<String>();
    crds.add("widgets.example.com");

    var response = new ChartCrds("repo/hello", "/tmp/hello", crds, "raw");

    crds.clear();
    assertEquals(1, response.customResourceDefinitions().size());
    assertThrows(
        UnsupportedOperationException.class,
        () -> response.customResourceDefinitions().add("new.example.com"));
  }

  @Test
  void searchResultSetDefensivelyCopiesCharts() {
    var charts = new ArrayList<ChartSummary>();
    charts.add(new ChartSummary("repo/nginx", "1.0.0", "2.1.0", "Nginx chart", 99));

    var response = new SearchResultSet(charts);

    charts.clear();
    assertEquals(1, response.size());
    assertThrows(UnsupportedOperationException.class, () -> response.charts().clear());
  }
}
