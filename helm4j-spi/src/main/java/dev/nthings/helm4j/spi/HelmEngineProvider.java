package dev.nthings.helm4j.spi;

/**
 * SPI entry point: constructs a {@link HelmEngine} for the host application.
 *
 * <p>Discovered via {@link java.util.ServiceLoader}. The {@code helm4j-runtime-native} module ships
 * the default FFM-backed provider; alternative runtimes only need to implement this interface and
 * be discoverable.
 */
public interface HelmEngineProvider {

  /**
   * Stable identifier for this runtime (e.g. {@code "native"}, {@code "process"}, {@code "fake"}).
   * Used by {@code HelmClientOptions.runtime(...)} to pick a specific provider when more than one
   * is on the module path.
   */
  String id();

  /** Creates a new engine, honoring whichever fields of {@code config} this runtime supports. */
  HelmEngine create(HelmEngineConfig config);
}
