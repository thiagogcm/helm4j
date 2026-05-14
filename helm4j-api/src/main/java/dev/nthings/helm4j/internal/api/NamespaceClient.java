package dev.nthings.helm4j.internal.api;

import java.util.Objects;

/** Base for the public namespace clients: holds the bounded gateway each one delegates to. */
public abstract class NamespaceClient<GatewayT> {

  protected final GatewayT gateway;

  protected NamespaceClient(GatewayT gateway) {
    this.gateway = Objects.requireNonNull(gateway, "gateway");
  }
}
