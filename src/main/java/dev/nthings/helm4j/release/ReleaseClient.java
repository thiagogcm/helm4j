package dev.nthings.helm4j.release;

import java.util.Objects;
import java.util.function.Consumer;

import dev.nthings.helm4j.internal.sdk.HelmGateway;

/** Release namespace for lifecycle operations. */
public final class ReleaseClient {

  private final HelmGateway gateway;

  public ReleaseClient(HelmGateway gateway) {
    this.gateway = Objects.requireNonNull(gateway, "gateway");
  }

  public InstallResult install(Consumer<InstallRequest.Builder> spec) {
    Objects.requireNonNull(spec, "spec");
    var builder = InstallRequest.builder();
    spec.accept(builder);
    return install(builder.build());
  }

  public InstallResult install(InstallRequest request) {
    Objects.requireNonNull(request, "request");
    return gateway.install(request);
  }
}
