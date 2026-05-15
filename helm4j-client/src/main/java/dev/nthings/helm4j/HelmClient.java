package dev.nthings.helm4j;

import java.util.Objects;
import java.util.ServiceLoader;

import dev.nthings.helm4j.client.charts.ChartsClient;
import dev.nthings.helm4j.client.registries.RegistriesClient;
import dev.nthings.helm4j.client.releases.ReleasesClient;
import dev.nthings.helm4j.client.repository.RepositoriesClient;
import dev.nthings.helm4j.client.system.SystemClient;
import dev.nthings.helm4j.errors.HelmConfigurationException;
import dev.nthings.helm4j.errors.HelmRuntimeException;
import dev.nthings.helm4j.spi.HelmEngine;
import dev.nthings.helm4j.spi.HelmEngineProvider;

/**
 * Application entry point for Helm4j.
 *
 * <p>{@code HelmClient} holds a {@link HelmEngine} and exposes namespace clients for the five Helm
 * domains: releases, charts, repositories, registries, system. A single client is safe to share
 * across threads.
 *
 * <pre>{@code
 * try (var helm = HelmClient.create()) {
 *   var release = helm.releases().install(InstallRelease.builder()
 *       .releaseName("nginx")
 *       .chart(ChartRef.repo("bitnami/nginx"))
 *       .namespace("apps")
 *       .build());
 * }
 * }</pre>
 *
 * <p>{@link AutoCloseable#close()} releases the engine. Always use try-with-resources.
 */
public final class HelmClient implements AutoCloseable {

  private final HelmEngine engine;
  private final ReleasesClient releases;
  private final ChartsClient charts;
  private final RepositoriesClient repositories;
  private final RegistriesClient registries;
  private final SystemClient system;

  private HelmClient(HelmEngine engine) {
    this.engine = Objects.requireNonNull(engine, "engine");
    this.releases = new ReleasesClient(engine.releases());
    this.charts = new ChartsClient(engine.charts());
    this.repositories = new RepositoriesClient(engine.repositories());
    this.registries = new RegistriesClient(engine.registries());
    this.system = new SystemClient(engine.system());
  }

  /**
   * Creates a client backed by the first {@link HelmEngineProvider} discovered via {@link
   * ServiceLoader} (typically the FFM-backed {@code helm4j-runtime-native} provider).
   *
   * @throws HelmRuntimeException if no provider is on the module path
   */
  public static HelmClient create() {
    return create(HelmClientOptions.builder().build());
  }

  /**
   * Creates a client with explicit options. If {@link HelmClientOptions#runtimeId()} is set, the
   * provider whose {@link HelmEngineProvider#id()} matches is chosen; otherwise the first provider
   * discovered wins.
   *
   * @throws HelmRuntimeException if no matching provider is on the module path
   * @throws HelmConfigurationException if the requested runtime id is unknown
   */
  public static HelmClient create(HelmClientOptions options) {
    Objects.requireNonNull(options, "options");
    var provider = resolveProvider(options);
    return new HelmClient(provider.create(options.toEngineConfig()));
  }

  /**
   * Advanced: wrap a caller-supplied {@link HelmEngine}, bypassing {@link ServiceLoader} discovery.
   * Intended for tests and alternate runtimes; production code should use {@link #create()}.
   */
  public static HelmClient using(HelmEngine engine) {
    return new HelmClient(engine);
  }

  private static HelmEngineProvider resolveProvider(HelmClientOptions options) {
    var loaded = ServiceLoader.load(HelmEngineProvider.class);
    if (options.runtimeId() != null) {
      for (var provider : loaded) {
        if (options.runtimeId().equals(provider.id())) {
          return provider;
        }
      }
      throw new HelmConfigurationException(
          "No HelmEngineProvider with id '" + options.runtimeId() + "' on the module path.");
    }
    return loaded
        .findFirst()
        .orElseThrow(
            () ->
                new HelmRuntimeException(
                    "No HelmEngineProvider found on the module path. Add the"
                        + " helm4j-runtime-native module."));
  }

  public ReleasesClient releases() {
    return releases;
  }

  public ChartsClient charts() {
    return charts;
  }

  public RepositoriesClient repositories() {
    return repositories;
  }

  public RegistriesClient registries() {
    return registries;
  }

  public SystemClient system() {
    return system;
  }

  @Override
  public void close() {
    engine.close();
  }
}
