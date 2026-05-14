package dev.nthings.helm4j.internal.api;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/** Shared helper for request-builder based namespace client operations. */
public final class ClientSupport {

  private ClientSupport() {}

  public static <B, RQ, RS> RS buildAndCall(
      Supplier<B> builderFactory,
      Consumer<B> spec,
      Function<B, RQ> requestBuilder,
      Function<RQ, RS> operation) {
    Objects.requireNonNull(builderFactory, "builderFactory");
    Objects.requireNonNull(spec, "spec");
    Objects.requireNonNull(requestBuilder, "requestBuilder");
    Objects.requireNonNull(operation, "operation");
    var builder = builderFactory.get();
    spec.accept(builder);
    return operation.apply(requestBuilder.apply(builder));
  }
}
