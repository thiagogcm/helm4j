import org.jspecify.annotations.NullMarked;

/**
 * Public, user-facing Helm SDK vocabulary: request and result records, sealed result hierarchies,
 * chart references, enums and the {@code HelmException} family. This module is pure data — it
 * carries no gateway SPI and no native plumbing. The gateway SPI and the client facade live in
 * {@code dev.nthings.helm4j.spi}; the FFM runtime lives in {@code dev.nthings.helm4j.runtime}.
 *
 * <p>The module is {@link NullMarked}: every type usage is non-null unless explicitly annotated
 * {@code @Nullable}. {@code @Nullable} is applied to the public surface — request and result record
 * components and exported method signatures. Internal mutable builder fields are intentionally left
 * unannotated; tightening those is a follow-up for whenever a nullness checker is wired into the
 * build.
 *
 * <p>{@code @SuppressWarnings("module")}: the qualified {@code opens ... to tools.jackson.databind}
 * names the Jackson module, which is not a compile dependency of this pure-data module — only the
 * native runtime that performs (de)serialization requires Jackson — so javac cannot see it at
 * compile time.
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

  // Jackson reflectively (de)serializes the DTO records in the native runtime module.
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
