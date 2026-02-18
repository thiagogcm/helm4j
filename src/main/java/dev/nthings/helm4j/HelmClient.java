package dev.nthings.helm4j;

import java.util.Objects;

import dev.nthings.helm4j.chart.ChartClient;
import dev.nthings.helm4j.internal.sdk.FfmHelmBridge;
import dev.nthings.helm4j.internal.sdk.HelmBridge;
import dev.nthings.helm4j.internal.sdk.HelmGateway;
import dev.nthings.helm4j.internal.sdk.NativeStructGateway;
import dev.nthings.helm4j.release.ReleaseClient;
import dev.nthings.helm4j.repo.RepoClient;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/** Root client for Helm SDK namespaces. */
public final class HelmClient implements AutoCloseable {

  private final HelmGateway gateway;
  private final RepoClient repo;
  private final ChartClient chart;
  private final ReleaseClient release;

  private HelmClient(HelmGateway gateway) {
    this.gateway = gateway;
    this.repo = new RepoClient(gateway);
    this.chart = new ChartClient(gateway);
    this.release = new ReleaseClient(gateway);
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
    return gateway.version();
  }

  @Override
  public void close() {
    // No-op for now. Native allocations are operation-scoped and released per call.
  }

  public static final class Builder {
    private ObjectMapper mapper;
    private HelmBridge bridge;

    private Builder() {
    }

    public Builder withObjectMapper(ObjectMapper value) {
      this.mapper = Objects.requireNonNull(value, "value");
      return this;
    }

    public Builder withBridge(HelmBridge value) {
      this.bridge = Objects.requireNonNull(value, "value");
      return this;
    }

    public HelmClient build() {
      var resolvedMapper = mapper == null ? defaultMapper() : mapper;
      var resolvedBridge = bridge == null ? new FfmHelmBridge() : bridge;
      return new HelmClient(new NativeStructGateway(resolvedBridge, resolvedMapper));
    }

    private static ObjectMapper defaultMapper() {
      return JsonMapper.builder()
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .build();
    }
  }
}
