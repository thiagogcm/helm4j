import org.jspecify.annotations.NullMarked;

/**
 * Gateway SPI and the user-facing client facade for the Helm SDK.
 *
 * <p>{@code dev.nthings.helm4j.spi} is the seam between the SDK and any runtime: a {@link
 * dev.nthings.helm4j.spi.HelmGatewayProvider} is discovered via {@link java.util.ServiceLoader}
 * (the {@code dev.nthings.helm4j.runtime} module supplies the FFM-backed implementation), but an
 * alternative runtime — a process-based gateway, an in-memory fake — only needs to implement this
 * SPI, not depend on the native module.
 *
 * <p>{@code dev.nthings.helm4j.client} is the public entry point: {@link
 * dev.nthings.helm4j.client.Helm} and {@link dev.nthings.helm4j.client.HelmClient}, with the
 * namespace clients under {@code dev.nthings.helm4j.client.{repo,chart,release}}.
 *
 * <p>The module is {@link NullMarked}.
 */
@NullMarked
module dev.nthings.helm4j.spi {
  requires transitive dev.nthings.helm4j;

  exports dev.nthings.helm4j.spi;
  exports dev.nthings.helm4j.client;
  exports dev.nthings.helm4j.client.repo;
  exports dev.nthings.helm4j.client.chart;
  exports dev.nthings.helm4j.client.release;

  uses dev.nthings.helm4j.spi.HelmGatewayProvider;
}
