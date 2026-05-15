import org.jspecify.annotations.NullMarked;

/**
 * Provider SPI for Helm4j runtimes.
 *
 * <p>{@code dev.nthings.helm4j.spi} is the seam between the SDK and any runtime: a {@link
 * dev.nthings.helm4j.spi.HelmEngineProvider} is discovered via {@link java.util.ServiceLoader} (the
 * {@code dev.nthings.helm4j.runtime.ffm} module supplies the default FFM-backed implementation).
 * Alternative runtimes — a process-based engine, an in-memory fake, a remote engine — only need to
 * implement this SPI; they do not depend on the native module.
 *
 * <p>Application developers depend on {@code dev.nthings.helm4j} (the client) rather than this
 * module directly.
 */
@NullMarked
module dev.nthings.helm4j.spi {
  requires transitive dev.nthings.helm4j.model;

  exports dev.nthings.helm4j.spi;
}
