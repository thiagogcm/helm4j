package dev.nthings.helm4j.spi;

/**
 * A Helm runtime: the seam between the client API and whichever process actually executes Helm
 * operations. The default implementation is FFM-backed; alternative implementations (process-based,
 * in-memory fakes, remote engines) implement this interface plus {@link HelmEngineProvider} for
 * discovery.
 *
 * <p>An engine fans out into five domain gateways. The accessor methods return stable instances for
 * the lifetime of the engine — callers may cache them.
 *
 * <p>Engines are {@link AutoCloseable}: callers obtain one through the client, hold it for as long
 * as they need it, and let try-with-resources release any underlying resources.
 */
public interface HelmEngine extends AutoCloseable {

  ReleaseGateway releases();

  ChartGateway charts();

  RepositoryGateway repositories();

  RegistryGateway registries();

  SystemGateway system();

  @Override
  default void close() {}
}
