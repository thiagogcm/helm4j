/**
 * Public, user-facing Helm SDK API. Native plumbing — including the JSON bridge layer — is supplied
 * by the helm4j-native module, discovered at runtime via {@link java.util.ServiceLoader}.
 *
 * <p>{@code @SuppressWarnings("module")}: the qualified {@code exports ... to
 * dev.nthings.helm4j.runtime} names the native module, which cannot be a compile dependency of this
 * module without a cycle, so javac cannot see it at compile time.
 */
@SuppressWarnings("module")
module dev.nthings.helm4j {
  // Jackson is referenced only by the `opens` directives below (for reflective DTO
  // (de)serialization performed by the native module); no API code imports it.
  requires static tools.jackson.databind;

  exports dev.nthings.helm4j;
  exports dev.nthings.helm4j.chart;
  exports dev.nthings.helm4j.release;
  exports dev.nthings.helm4j.repo;
  exports dev.nthings.helm4j.model;
  exports dev.nthings.helm4j.errors;

  // Gateway interfaces and the native SPI are visible only to the runtime module.
  exports dev.nthings.helm4j.internal.spi to
      dev.nthings.helm4j.runtime;

  uses dev.nthings.helm4j.internal.spi.HelmGatewayProvider;

  // Jackson reflectively (de)serializes the DTO records.
  opens dev.nthings.helm4j to
      tools.jackson.databind;
  opens dev.nthings.helm4j.chart to
      tools.jackson.databind;
  opens dev.nthings.helm4j.release to
      tools.jackson.databind;
  opens dev.nthings.helm4j.repo to
      tools.jackson.databind;
  opens dev.nthings.helm4j.model to
      tools.jackson.databind;
}
