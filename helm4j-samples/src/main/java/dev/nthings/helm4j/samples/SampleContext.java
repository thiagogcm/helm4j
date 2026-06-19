package dev.nthings.helm4j.samples;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import dev.nthings.helm4j.HelmClient;

/**
 * Shared state handed to every {@link Sample}: the open {@link HelmClient}, the chart used by the
 * offline samples, the namespace the cluster samples operate in, and a temp-directory factory that
 * cleans up on JVM exit.
 */
public final class SampleContext {

  /** Namespace the cluster-bound samples install into. */
  public static final String NAMESPACE = "helm4j-samples";

  private final HelmClient helm;
  private final Path chartPath;

  public SampleContext(HelmClient helm, Path chartPath) {
    this.helm = helm;
    this.chartPath = chartPath;
  }

  /** The shared client; stable for the lifetime of the run. */
  public HelmClient helm() {
    return helm;
  }

  /** Filesystem path to the bundled {@code hello-world} chart. */
  public Path chartPath() {
    return chartPath;
  }

  /** Namespace the cluster samples install into. */
  public String namespace() {
    return NAMESPACE;
  }

  /**
   * Creates a temp directory that is recursively deleted when the JVM exits. Used by samples that
   * write artifacts to disk (e.g. {@code helm package}).
   */
  public Path newTempDirectory(String prefix) {
    try {
      var dir = Files.createTempDirectory(prefix);
      deleteOnExit(dir);
      return dir;
    } catch (IOException e) {
      throw new UncheckedIOException("could not create temp directory " + prefix, e);
    }
  }

  /**
   * Resolves the bundled chart path from the {@code helm4j.samples.chart} system property, falling
   * back to the in-repo location used during development.
   */
  public static Path locateChart() {
    var configured = System.getProperty("helm4j.samples.chart");
    if (configured != null && !configured.isBlank()) {
      return Path.of(configured);
    }
    return Path.of("helm4j-samples/src/main/resources/charts/hello-world");
  }

  private static void deleteOnExit(Path root) {
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  try (Stream<Path> walk = Files.walk(root)) {
                    walk.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
                  } catch (IOException ignored) {
                    // best-effort cleanup
                  }
                },
                "helm4j-samples-cleanup"));
  }
}
