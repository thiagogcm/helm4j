import org.jspecify.annotations.NullMarked;

/**
 * Public Helm SDK vocabulary: request/response records, value types, enums, and the {@code
 * HelmException} family. Pure data — no client, SPI, or native plumbing. Every Helm4j consumer and
 * every runtime provider depends on this module.
 */
@NullMarked
module dev.nthings.helm4j.model {
  requires transitive org.jspecify;

  exports dev.nthings.helm4j.auth;
  exports dev.nthings.helm4j.chart;
  exports dev.nthings.helm4j.release;
  exports dev.nthings.helm4j.repository;
  exports dev.nthings.helm4j.registry;
  exports dev.nthings.helm4j.system;
  exports dev.nthings.helm4j.model;
  exports dev.nthings.helm4j.errors;
}
