package dev.nthings.helm4j;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import dev.nthings.helm4j.chart.ChartRef;
import dev.nthings.helm4j.chart.ShowMode;
import dev.nthings.helm4j.errors.HelmException;
import dev.nthings.helm4j.model.ListResult;
import dev.nthings.helm4j.release.ApplyStrategy;
import dev.nthings.helm4j.release.DryRunMode;
import dev.nthings.helm4j.release.InstallRequest;
import dev.nthings.helm4j.release.UpgradeRequest;
import dev.nthings.helm4j.release.WaitMode;
import dev.nthings.helm4j.repo.RepoAddRequest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the public namespace clients and fluent builders against an in-module {@link
 * FakeHelmGateway}, covering the API surface without the native runtime.
 */
@DisplayName("HelmClient API: namespace client delegation and fluent builders")
class HelmClientApiTest {

  @Test
  void helmClientExposesNamespacesAndVersion() {
    var fake = new FakeHelmGateway();
    try (var helm = HelmClient.using(fake)) {
      assertNotNull(helm.repo());
      assertNotNull(helm.chart());
      assertNotNull(helm.release());
      assertSame(helm.repo(), helm.repo());

      var version = helm.version();
      assertEquals("0.1.0", version.version());
      assertEquals("go1.26", version.goVersion());
      assertEquals("v4.1.1", version.helmVersion());
    }
  }

  @Test
  void repoClientDelegatesEveryOperation() {
    var fake = new FakeHelmGateway();
    var repo = HelmClient.using(fake).repo();

    repo.add(spec -> spec.name("bitnami").url("https://charts.bitnami.com/bitnami"));
    assertInstanceOf(RepoAddRequest.class, fake.lastRequest);

    repo.update();
    repo.update(spec -> spec.names("bitnami").timeout(Duration.ofSeconds(20)));
    assertEquals(0, repo.update(spec -> spec.names("bitnami")).size());

    assertEquals(0, repo.list().size());
    assertEquals(0, repo.remove(spec -> spec.names("bitnami")).size());

    repo.registryLogin(spec -> spec.hostname("registry.example").username("u").password("p"));
    repo.registryLogout(spec -> spec.hostname("registry.example"));
  }

  @Test
  void chartClientDelegatesEveryOperation() {
    var fake = new FakeHelmGateway();
    var chart = HelmClient.using(fake).chart();
    var chartRef = ChartRef.repo("bitnami/nginx");

    assertEquals(0, chart.searchRepo(spec -> spec.keyword("nginx")).size());
    assertEquals(0, chart.searchHub(spec -> spec.keyword("nginx")).size());

    for (var mode : ShowMode.values()) {
      chart.show(mode, chartRef, spec -> {});
      assertEquals(mode.wireValue(), fake.lastShowMode);
    }

    chart.template(spec -> spec.releaseName("nginx").chart(chartRef));
    chart.lint(spec -> spec.chartPath(Path.of("/tmp/chart")));
    chart.pull(spec -> spec.chartReference("bitnami/nginx"));
    chart.push(spec -> spec.chartReference("/tmp/nginx.tgz").remote("oci://registry.example"));
    chart.packageChart(spec -> spec.chartPath(Path.of("/tmp/chart")));
    chart.dependency(spec -> spec.chartPath(Path.of("/tmp/chart")));
  }

  @Test
  void releaseClientDelegatesEveryOperation() {
    var fake = new FakeHelmGateway();
    var release = HelmClient.using(fake).release();
    var chartRef = ChartRef.repo("bitnami/nginx");

    release.install(spec -> spec.releaseName("nginx").chart(chartRef));
    release.upgrade(spec -> spec.releaseName("nginx").chart(chartRef));
    release.uninstall(spec -> spec.releaseName("nginx"));
    release.status(spec -> spec.releaseName("nginx"));
    release.rollback(spec -> spec.releaseName("nginx").revision(1));
    assertEquals(0, release.history(spec -> spec.releaseName("nginx")).size());

    assertEquals(0, release.list().size());
    assertEquals(0, release.list(spec -> {}).size());

    release.test(spec -> spec.releaseName("nginx"));
    release.getAll(spec -> spec.releaseName("nginx"));
    release.getValues(spec -> spec.releaseName("nginx"));
    release.getManifest(spec -> spec.releaseName("nginx"));
    release.getHooks(spec -> spec.releaseName("nginx"));
    release.getNotes(spec -> spec.releaseName("nginx"));
    release.getMetadata(spec -> spec.releaseName("nginx"));

    assertEquals(15, fake.invocations);
  }

  @Test
  @DisplayName("Helm fluent install/upgrade builders delegate to the provided client")
  void fluentBuildersDelegateToClient() {
    var fake = new FakeHelmGateway();
    try (var helm = HelmClient.using(fake)) {
      Helm.install(ChartRef.repo("bitnami/nginx"))
          .releaseName("nginx")
          .version("19.0.0")
          .namespace("apps")
          .createNamespace(true)
          .dryRun(DryRunMode.NONE)
          .waitMode(WaitMode.WATCHER)
          .timeout(Duration.ofMinutes(3))
          .applyStrategy(ApplyStrategy.SERVER_SIDE_APPLY_FORCE_CONFLICTS)
          .values(Map.of("replicaCount", 3))
          .labels(Map.of("team", "platform"))
          .run(helm);
      assertInstanceOf(InstallRequest.class, fake.lastRequest);

      Helm.upgrade(ChartRef.oci("oci://registry-1.docker.io/bitnamicharts/nginx"))
          .releaseName("nginx")
          .version("19.0.0")
          .namespace("apps")
          .install(true)
          .dryRun(DryRunMode.NONE)
          .waitMode(WaitMode.WATCHER)
          .timeout(Duration.ofMinutes(3))
          .applyStrategy(ApplyStrategy.SERVER_SIDE_APPLY)
          .values(Map.of("replicaCount", 3))
          .labels(Map.of("team", "platform"))
          .run(helm);
      assertInstanceOf(UpgradeRequest.class, fake.lastRequest);
    }
  }

  @Test
  void listResultExposesSizeAndFirst() {
    var empty = ListResult.<String>of(List.of());
    assertEquals(0, empty.size());
    assertTrue(empty.first().isEmpty());

    var populated = ListResult.of(List.of("a", "b"));
    assertEquals(2, populated.size());
    assertEquals("a", populated.first().orElseThrow());
  }

  @Test
  void helmExceptionCarriesStageAndOperation() {
    var error = new HelmException("boom", "runOperation", "install");
    assertEquals("boom", error.getMessage());
    assertEquals("runOperation", error.stage());
    assertEquals("install", error.operation());

    var cause = new IllegalStateException("root");
    var wrapped = new HelmException("boom", "runOperation", "install", cause);
    assertSame(cause, wrapped.getCause());

    assertThrows(
        HelmException.class,
        () -> {
          throw error;
        });
  }
}
