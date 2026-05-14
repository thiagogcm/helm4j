package dev.nthings.helm4j.internal.gateway;

/**
 * Service provider that constructs a fully wired {@link HelmGateway}.
 *
 * <p>Discovered by {@code HelmClient} through {@link java.util.ServiceLoader}. The implementation
 * lives in the native runtime module so that the public API module carries no compile-time
 * reference to the FFM plumbing — including the JSON layer it uses internally.
 */
public interface HelmGatewayProvider {

  /** Creates a gateway backed by the native Helm runtime. */
  HelmGateway create();
}
