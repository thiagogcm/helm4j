package dev.nthings.helm4j.internal.api;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import dev.nthings.helm4j.internal.sdk.HelmGateway;

/** Shared behavior for public namespace clients backed by {@link HelmGateway}. */
public abstract class NamespaceClient {

  protected final HelmGateway gateway;

  protected NamespaceClient(HelmGateway gateway) {
    this.gateway = Objects.requireNonNull(gateway, "gateway");
  }

  protected final <BuilderT, RequestT, ResultT> ResultT buildAndInvoke(
      Supplier<BuilderT> builderFactory,
      Consumer<BuilderT> spec,
      Function<BuilderT, RequestT> build,
      Function<RequestT, ResultT> operation) {
    return ClientSupport.buildAndCall(builderFactory, spec, build, operation);
  }

  protected final <RequestT, ResultT> ResultT invoke(
      RequestT request, Function<RequestT, ResultT> operation) {
    return operation.apply(Objects.requireNonNull(request, "request"));
  }
}
