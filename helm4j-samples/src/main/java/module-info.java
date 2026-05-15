/**
 * Runnable samples that consume the public helm4j client API end to end.
 *
 * <p>This module depends only on {@code dev.nthings.helm4j.spi} — the same surface a real consumer
 * compiles against. {@code dev.nthings.helm4j.runtime} is supplied at runtime and discovered via
 * {@link java.util.ServiceLoader}; it is intentionally not a compile dependency.
 */
module dev.nthings.helm4j.samples {
  requires dev.nthings.helm4j.spi;
}
