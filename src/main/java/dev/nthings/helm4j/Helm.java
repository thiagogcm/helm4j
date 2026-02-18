package dev.nthings.helm4j;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import dev.nthings.helm4j.release.ApplyStrategy;
import dev.nthings.helm4j.release.DryRunMode;
import dev.nthings.helm4j.release.InstallRequest;
import dev.nthings.helm4j.release.InstallResult;
import dev.nthings.helm4j.release.WaitMode;
import dev.nthings.helm4j.types.ChartRef;

/** Standard static SDK entrypoint for Helm operations. */
public final class Helm {

  private Helm() {
  }

  public static HelmClient client() {
    return HelmClient.builder().build();
  }

  public static HelmClient client(Consumer<HelmClient.Builder> spec) {
    Objects.requireNonNull(spec, "spec");
    var builder = HelmClient.builder();
    spec.accept(builder);
    return builder.build();
  }

  /** Begin a fluent install operation for the given chart reference. */
  public static InstallBuilder install(ChartRef chart) {
    return new InstallBuilder(chart);
  }

  /** Begin a fluent install operation for a repository chart reference. */
  public static InstallBuilder install(String repoChart) {
    return new InstallBuilder(ChartRef.repo(repoChart));
  }

  /** Fluent builder for one-shot install operations using a default client. */
  public static final class InstallBuilder {
    private final InstallRequest.Builder requestBuilder = InstallRequest.builder();

    private InstallBuilder(ChartRef chart) {
      requestBuilder.chart(chart);
    }

    public InstallBuilder releaseName(String value) {
      requestBuilder.releaseName(value);
      return this;
    }

    public InstallBuilder version(String value) {
      requestBuilder.source(s -> s.version(value));
      return this;
    }

    public InstallBuilder namespace(String value) {
      requestBuilder.namespace(value);
      return this;
    }

    public InstallBuilder createNamespace(boolean value) {
      requestBuilder.createNamespace(value);
      return this;
    }

    public InstallBuilder dryRun(DryRunMode value) {
      requestBuilder.dryRunMode(value);
      return this;
    }

    public InstallBuilder waitMode(WaitMode value) {
      requestBuilder.waitMode(value);
      return this;
    }

    public InstallBuilder timeout(Duration value) {
      requestBuilder.timeout(value);
      return this;
    }

    public InstallBuilder applyStrategy(ApplyStrategy value) {
      requestBuilder.applyStrategy(value);
      return this;
    }

    public InstallBuilder values(Map<String, Object> value) {
      requestBuilder.values(value);
      return this;
    }

    public InstallBuilder labels(Map<String, String> value) {
      requestBuilder.labels(value);
      return this;
    }

    /** Execute the install operation using a default client. */
    public InstallResult run() {
      try (var helm = Helm.client()) {
        return helm.release().install(requestBuilder.build());
      }
    }

    /** Execute the install operation using the provided client. */
    public InstallResult run(HelmClient client) {
      Objects.requireNonNull(client, "client");
      return client.release().install(requestBuilder.build());
    }
  }
}
