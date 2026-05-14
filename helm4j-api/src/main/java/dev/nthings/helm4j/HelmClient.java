package dev.nthings.helm4j;

import java.util.Objects;
import java.util.ServiceLoader;

import dev.nthings.helm4j.chart.ChartClient;
import dev.nthings.helm4j.internal.spi.ChartGateway;
import dev.nthings.helm4j.internal.spi.HelmGateway;
import dev.nthings.helm4j.internal.spi.HelmGatewayProvider;
import dev.nthings.helm4j.internal.spi.ReleaseGateway;
import dev.nthings.helm4j.internal.spi.RepoGateway;
import dev.nthings.helm4j.internal.spi.SystemGateway;
import dev.nthings.helm4j.release.ReleaseClient;
import dev.nthings.helm4j.repo.RepoClient;

/** Root client for Helm SDK namespaces. */
public final class HelmClient implements AutoCloseable {

  private final SystemGateway system;
  private final RepoClient repo;
  private final ChartClient chart;
  private final ReleaseClient release;

  private HelmClient(
      RepoGateway repoGateway,
      ChartGateway chartGateway,
      ReleaseGateway releaseGateway,
      SystemGateway systemGateway) {
    this.system = systemGateway;
    this.repo = new RepoClient(repoGateway);
    this.chart = new ChartClient(chartGateway);
    this.release = new ReleaseClient(releaseGateway);
  }

  /** Creates a client backed by the native Helm runtime discovered on the module path. */
  public static HelmClient create() {
    return using(ProviderHolder.INSTANCE.create());
  }

  /**
   * Wraps a caller-supplied {@link HelmGateway} directly, bypassing native gateway discovery.
   *
   * <p>{@link HelmGateway} is only exported to the native runtime module, so this entry point is
   * not reachable from the consumer-facing API surface; it exists for the runtime module and its
   * tests.
   */
  public static HelmClient using(HelmGateway gateway) {
    Objects.requireNonNull(gateway, "gateway");
    return new HelmClient(gateway, gateway, gateway, gateway);
  }

  public RepoClient repo() {
    return repo;
  }

  public ChartClient chart() {
    return chart;
  }

  public ReleaseClient release() {
    return release;
  }

  public VersionInfo version() {
    return system.version();
  }

  @Override
  public void close() {
    // No-op for now. Native allocations are operation-scoped and released per call.
  }

  /**
   * Lazy-initialization holder for the {@link HelmGatewayProvider} resolved via {@link
   * ServiceLoader}. The native runtime module supplies the implementation; a resolution failure
   * means it is missing from the module path.
   */
  private static final class ProviderHolder {
    static final HelmGatewayProvider INSTANCE =
        ServiceLoader.load(HelmGatewayProvider.class)
            .findFirst()
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "No HelmGatewayProvider found on the module path. Add the helm4j-native"
                            + " module."));
  }
}
