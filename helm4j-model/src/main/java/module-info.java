import org.jspecify.annotations.NullMarked;

/**
 * Public Helm SDK vocabulary: request/response records, value types, enums, and the {@code
 * HelmException} family. Pure data — no client, SPI, or native plumbing. Every Helm4j consumer and
 * every runtime provider depends on this module.
 *
 * <p>{@code @SuppressWarnings("module")}: the qualified {@code opens ... to tools.jackson.databind}
 * names the Jackson module, which is not a compile dependency of this pure-data module — only the
 * native runtime that performs (de)serialization requires Jackson — so javac cannot see it at
 * compile time.
 */
@SuppressWarnings("module")
@NullMarked
module dev.nthings.helm4j.model {
  requires transitive org.jspecify;

  exports dev.nthings.helm4j.chart;
  exports dev.nthings.helm4j.release;
  exports dev.nthings.helm4j.repository;
  exports dev.nthings.helm4j.registry;
  exports dev.nthings.helm4j.system;
  exports dev.nthings.helm4j.model;
  exports dev.nthings.helm4j.errors;

  // Jackson reflectively (de)serializes the DTO records in the native runtime module.
  opens dev.nthings.helm4j.chart to
      tools.jackson.databind;
  opens dev.nthings.helm4j.release to
      tools.jackson.databind;
  opens dev.nthings.helm4j.repository to
      tools.jackson.databind;
  opens dev.nthings.helm4j.registry to
      tools.jackson.databind;
  opens dev.nthings.helm4j.system to
      tools.jackson.databind;
  opens dev.nthings.helm4j.model to
      tools.jackson.databind;
}
