package dev.nthings.helm4j.client.internal;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Base for the public namespace clients: holds the bounded gateway each one delegates to. */
public abstract class NamespaceClient<GatewayT> {

  protected final GatewayT gateway;

  protected NamespaceClient(GatewayT gateway) {
    this.gateway = Objects.requireNonNull(gateway, "gateway");
  }

  /** Creates a fresh request builder and applies the caller's configuration to it. */
  protected static <B> B configured(Supplier<B> builderFactory, Consumer<B> spec) {
    var builder = builderFactory.get();
    spec.accept(builder);
    return builder;
  }
}
