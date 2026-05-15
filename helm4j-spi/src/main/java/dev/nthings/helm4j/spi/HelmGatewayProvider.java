package dev.nthings.helm4j.spi;

/**
 * SPI entry point: constructs a fully wired {@link HelmGateway}.
 *
 * <p>Discovered by {@code HelmClient} through {@link java.util.ServiceLoader}. The {@code
 * helm4j-native} module supplies the FFM-backed implementation, so the SDK carries no compile-time
 * reference to the native plumbing — including the JSON layer it uses internally. An alternative
 * runtime only needs to implement this SPI and be discoverable.
 */
public interface HelmGatewayProvider {

  /** Creates a gateway backed by the native Helm runtime. */
  HelmGateway create();
}
