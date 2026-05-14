package dev.nthings.helm4j.internal.api;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/** Shared behavior for public namespace clients backed by a bounded gateway context. */
public abstract class NamespaceClient<GatewayT> {

  protected final GatewayT gateway;

  protected NamespaceClient(GatewayT gateway) {
    this.gateway = Objects.requireNonNull(gateway, "gateway");
  }

  protected final <BuilderT, RequestT, ResultT> ResultT buildAndInvoke(
      Supplier<BuilderT> builderFactory,
      Consumer<BuilderT> spec,
      Function<BuilderT, RequestT> build,
      Function<RequestT, ResultT> operation) {
    Objects.requireNonNull(builderFactory, "builderFactory");
    Objects.requireNonNull(spec, "spec");
    Objects.requireNonNull(build, "build");
    Objects.requireNonNull(operation, "operation");
    var builder = builderFactory.get();
    spec.accept(builder);
    return operation.apply(build.apply(builder));
  }

  protected final <RequestT, ResultT> ResultT invoke(
      RequestT request, Function<RequestT, ResultT> operation) {
    return operation.apply(Objects.requireNonNull(request, "request"));
  }
}
