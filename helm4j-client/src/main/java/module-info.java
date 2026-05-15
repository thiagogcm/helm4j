import org.jspecify.annotations.NullMarked;

/**
 * The Helm4j consumer client.
 *
 * <p>Applications add {@code dev.nthings.helm4j} (this module) to their compile path and a runtime
 * provider (typically {@code dev.nthings.helm4j.runtime.ffm}) to their runtime path:
 *
 * <pre>{@code
 * dependencies {
 *     implementation("dev.nthings.helm4j:helm4j-client")
 *     runtimeOnly("dev.nthings.helm4j:helm4j-runtime-native")
 * }
 * }</pre>
 *
 * <p>Application module declaration: {@code requires dev.nthings.helm4j;}.
 *
 * <p>The runtime provider is discovered through {@link java.util.ServiceLoader}.
 */
@NullMarked
module dev.nthings.helm4j {
  requires transitive dev.nthings.helm4j.model;
  requires transitive dev.nthings.helm4j.spi;

  exports dev.nthings.helm4j;
  exports dev.nthings.helm4j.client.releases;
  exports dev.nthings.helm4j.client.charts;
  exports dev.nthings.helm4j.client.repository;
  exports dev.nthings.helm4j.client.registries;
  exports dev.nthings.helm4j.client.system;

  uses dev.nthings.helm4j.spi.HelmEngineProvider;
}
