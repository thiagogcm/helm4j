package dev.nthings.helm4j.types;

import java.nio.file.Path;
import java.util.Objects;

/** Local filesystem chart reference. */
public record LocalChartRef(Path path) implements ChartRef {

  public LocalChartRef {
    path = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
  }

  @Override
  public String asReference() {
    return path.toString();
  }
}
