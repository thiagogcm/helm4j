package dev.nthings.helm4j;

import java.util.Objects;

import dev.nthings.helm4j.chart.ChartClient;
import dev.nthings.helm4j.internal.sdk.ChartGateway;
import dev.nthings.helm4j.internal.sdk.FfmHelmBridge;
import dev.nthings.helm4j.internal.sdk.HelmBridge;
import dev.nthings.helm4j.internal.sdk.HelmGateway;
import dev.nthings.helm4j.internal.sdk.NativeStructGateway;
import dev.nthings.helm4j.internal.sdk.ReleaseGateway;
import dev.nthings.helm4j.internal.sdk.RepoGateway;
import dev.nthings.helm4j.internal.sdk.SystemGateway;
import dev.nthings.helm4j.release.ReleaseClient;
import dev.nthings.helm4j.repo.RepoClient;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

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

  public static Builder builder() {
    return new Builder();
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

  public static final class Builder {
    private ObjectMapper mapper;
    private HelmBridge bridge;
    private HelmGateway gateway;

    private Builder() {}

    public Builder withObjectMapper(ObjectMapper value) {
      this.mapper = Objects.requireNonNull(value, "value");
      return this;
    }

    public Builder withBridge(HelmBridge value) {
      this.bridge = Objects.requireNonNull(value, "value");
      return this;
    }

    public Builder withGateway(HelmGateway value) {
      this.gateway = Objects.requireNonNull(value, "value");
      return this;
    }

    public HelmClient build() {
      var resolvedMapper = mapper == null ? DefaultMapperHolder.INSTANCE : mapper;
      var resolvedGateway = gateway == null ? nativeGateway(resolvedMapper) : gateway;
      return new HelmClient(resolvedGateway, resolvedGateway, resolvedGateway, resolvedGateway);
    }

    /**
     * Lazy-initialization holder for the shared default {@link ObjectMapper}. The mapper is
     * stateless w.r.t. configuration, so a single instance is safe to share across all {@link
     * Helm#client()} calls. Holder pattern keeps initialization cost off the class load path of
     * {@link HelmClient}.
     */
    private static final class DefaultMapperHolder {
      static final ObjectMapper INSTANCE =
          JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
    }

    private HelmGateway nativeGateway(ObjectMapper resolvedMapper) {
      var resolvedBridge = bridge == null ? new FfmHelmBridge() : bridge;
      return new NativeStructGateway(resolvedBridge, resolvedMapper);
    }
  }
}
