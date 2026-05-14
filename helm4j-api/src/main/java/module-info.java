import org.jspecify.annotations.NullMarked;

/**
 * Public, user-facing Helm SDK API. Native plumbing — including the JSON bridge layer — is supplied
 * by the helm4j-native module, discovered at runtime via {@link java.util.ServiceLoader}.
 *
 * <p>The module is {@link NullMarked}: every type usage is non-null unless explicitly annotated
 * {@code @Nullable}.
 *
 * <p>{@code @SuppressWarnings("module")}: the qualified {@code exports ... to
 * dev.nthings.helm4j.runtime} names the native module, which cannot be a compile dependency of this
 * module without a cycle, so javac cannot see it at compile time.
 */
@SuppressWarnings("module")
@NullMarked
module dev.nthings.helm4j {
  requires transitive org.jspecify;

  exports dev.nthings.helm4j;
  exports dev.nthings.helm4j.chart;
  exports dev.nthings.helm4j.release;
  exports dev.nthings.helm4j.repo;
  exports dev.nthings.helm4j.model;
  exports dev.nthings.helm4j.errors;

  // The gateway SPI is the internal seam consumed by the native runtime module only.
  exports dev.nthings.helm4j.internal.gateway to
      dev.nthings.helm4j.runtime;

  uses dev.nthings.helm4j.internal.gateway.HelmGatewayProvider;

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
