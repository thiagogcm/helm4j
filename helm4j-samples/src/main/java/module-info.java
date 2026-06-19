/**
 * Runnable samples that consume the public helm4j client API end to end.
 *
 * <p>This module compiles against {@code dev.nthings.helm4j} (the client surface a real application
 * uses) and {@code org.slf4j} (the logging facade the samples narrate through). The SLF4J backend
 * ({@code log4j2}) and {@code dev.nthings.helm4j.runtime.ffm} are supplied at runtime and discovered
 * via {@link java.util.ServiceLoader}; neither is a compile dependency.
 */
module dev.nthings.helm4j.samples {
  requires dev.nthings.helm4j;
  requires org.slf4j;
}
