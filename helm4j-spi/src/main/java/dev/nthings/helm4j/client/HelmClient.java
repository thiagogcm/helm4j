package dev.nthings.helm4j.client;

import java.util.Objects;
import java.util.ServiceLoader;

import dev.nthings.helm4j.VersionInfo;
import dev.nthings.helm4j.client.chart.ChartClient;
import dev.nthings.helm4j.client.release.ReleaseClient;
import dev.nthings.helm4j.client.repo.RepoClient;
import dev.nthings.helm4j.spi.ChartGateway;
import dev.nthings.helm4j.spi.HelmGateway;
import dev.nthings.helm4j.spi.HelmGatewayProvider;
import dev.nthings.helm4j.spi.ReleaseGateway;
import dev.nthings.helm4j.spi.RepoGateway;
import dev.nthings.helm4j.spi.SystemGateway;

/**
 * Root client for Helm SDK namespaces.
 *
 * <p>{@code HelmClient} and its namespace clients ({@code repo()}, {@code chart()}, {@code
 * release()}) are immutable and stateless, so a single instance is safe to share across threads.
 *
 * <p>The class is {@link AutoCloseable} and should be used with try-with-resources for forward
 * compatibility, but {@link #close()} is currently a no-op: native allocations are scoped to and
 * released by each individual operation.
 */
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
   * <p>This is the SPI extension point: it lets callers drive the SDK with an alternative {@link
   * HelmGateway} implementation — a process-based gateway, an in-memory fake, or a test double —
   * without the native runtime module on the module path.
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

  /**
   * Currently a no-op — native allocations are released per operation. Declared for forward
   * compatibility; always use {@code HelmClient} with try-with-resources.
   */
  @Override
  public void close() {}

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
