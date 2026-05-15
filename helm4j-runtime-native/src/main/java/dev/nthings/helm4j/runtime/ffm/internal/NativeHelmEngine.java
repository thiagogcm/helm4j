package dev.nthings.helm4j.runtime.ffm.internal;

import java.util.Objects;

import dev.nthings.helm4j.spi.ChartGateway;
import dev.nthings.helm4j.spi.HelmEngine;
import dev.nthings.helm4j.spi.HelmEngineConfig;
import dev.nthings.helm4j.spi.RegistryGateway;
import dev.nthings.helm4j.spi.ReleaseGateway;
import dev.nthings.helm4j.spi.RepositoryGateway;
import dev.nthings.helm4j.spi.SystemGateway;

import tools.jackson.databind.ObjectMapper;

/**
 * FFM-backed {@link HelmEngine}: wires the shared {@link NativeGatewaySupport} into one sub-gateway
 * per domain and surfaces them through the engine accessors.
 */
public final class NativeHelmEngine implements HelmEngine {

  private final NativeRepositoryGateway repositories;
  private final NativeRegistryGateway registries;
  private final NativeChartGateway charts;
  private final NativeReleaseGateway releases;
  private final NativeSystemGateway system;

  public NativeHelmEngine(HelmBridge bridge, ObjectMapper mapper, HelmEngineConfig config) {
    Objects.requireNonNull(bridge, "bridge");
    Objects.requireNonNull(mapper, "mapper");
    // config.kubeContext() is accepted but not consumed here — the native bridge takes
    // kube-context per request, so engine-level context is not honored yet.
    Objects.requireNonNull(config, "config");
    var support = new NativeGatewaySupport(bridge, mapper);
    this.repositories = new NativeRepositoryGateway(support);
    this.registries = new NativeRegistryGateway(support);
    this.charts = new NativeChartGateway(support);
    this.releases = new NativeReleaseGateway(support);
    this.system = new NativeSystemGateway(support);
  }

  @Override
  public ReleaseGateway releases() {
    return releases;
  }

  @Override
  public ChartGateway charts() {
    return charts;
  }

  @Override
  public RepositoryGateway repositories() {
    return repositories;
  }

  @Override
  public RegistryGateway registries() {
    return registries;
  }

  @Override
  public SystemGateway system() {
    return system;
  }
}
