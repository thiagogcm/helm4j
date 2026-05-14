package dev.nthings.helm4j;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import dev.nthings.helm4j.chart.ChartRef;
import dev.nthings.helm4j.chart.ShowMode;
import dev.nthings.helm4j.errors.HelmException;
import dev.nthings.helm4j.errors.HelmFailure;
import dev.nthings.helm4j.model.ListResult;
import dev.nthings.helm4j.release.InstallRequest;
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
 * Exercises the public namespace clients and their consumer-based fluent entry points against an
 * in-module {@link FakeHelmGateway}, covering the API surface without the native runtime.
 */
@DisplayName("HelmClient API: namespace client delegation and fluent request builders")
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

    repo.add(b -> b.name("bitnami").url("https://charts.bitnami.com/bitnami"));
    assertInstanceOf(RepoAddRequest.class, fake.lastRequest);

    repo.update(b -> {});
    repo.update(b -> b.names("bitnami").timeout(Duration.ofSeconds(20)));
    assertEquals(0, repo.update(b -> b.names("bitnami")).size());

    assertEquals(0, repo.list().size());
    assertEquals(0, repo.remove(b -> b.names("bitnami")).size());

    repo.registryLogin(b -> b.hostname("registry.example").username("u").password("p"));
    repo.registryLogout(b -> b.hostname("registry.example"));
  }

  @Test
  void chartClientDelegatesEveryOperation() {
    var fake = new FakeHelmGateway();
    var chart = HelmClient.using(fake).chart();
    var chartRef = ChartRef.repo("bitnami/nginx");

    assertEquals(0, chart.searchRepo(b -> b.keyword("nginx")).size());
    assertEquals(0, chart.searchHub(b -> b.keyword("nginx")).size());

    for (var mode : ShowMode.values()) {
      chart.show(mode, chartRef, b -> {});
      assertEquals(mode.wireValue(), fake.lastShowMode);
    }

    chart.template(b -> b.releaseName("nginx").chart(chartRef));
    chart.lint(b -> b.chartPath(Path.of("/tmp/chart")));
    chart.pull(b -> b.chart(ChartRef.repo("bitnami/nginx")));
    chart.push(b -> b.chartReference("/tmp/nginx.tgz").remote("oci://registry.example"));
    chart.packageChart(b -> b.chartPath(Path.of("/tmp/chart")));
    chart.dependency(b -> b.chartPath(Path.of("/tmp/chart")));
  }

  @Test
  void releaseClientDelegatesEveryOperation() {
    var fake = new FakeHelmGateway();
    var release = HelmClient.using(fake).release();
    var chartRef = ChartRef.repo("bitnami/nginx");

    release.install(b -> b.releaseName("nginx").chart(chartRef));
    release.upgrade(b -> b.releaseName("nginx").chart(chartRef));
    release.uninstall(b -> b.releaseName("nginx"));
    release.status(b -> b.releaseName("nginx"));
    release.rollback(b -> b.releaseName("nginx").revision(1));
    assertEquals(0, release.history(b -> b.releaseName("nginx")).size());

    assertEquals(0, release.list(b -> {}).size());

    release.test(b -> b.releaseName("nginx"));
    release.getAll(b -> b.releaseName("nginx"));
    release.getValues(b -> b.releaseName("nginx"));
    release.getManifest(b -> b.releaseName("nginx"));
    release.getHooks(b -> b.releaseName("nginx"));
    release.getNotes(b -> b.releaseName("nginx"));
    release.getMetadata(b -> b.releaseName("nginx"));

    assertEquals(14, fake.invocations);
  }

  @Test
  @DisplayName("Pre-built requests can still be passed to the client overloads")
  void preBuiltRequestsDelegateToClient() {
    var fake = new FakeHelmGateway();
    var release = HelmClient.using(fake).release();
    var request = InstallRequest.builder().releaseName("nginx").chart(ChartRef.repo("a/b")).build();
    release.install(request);
    assertSame(request, fake.lastRequest);
  }

  @Test
  void listResultIsIterableAndStreamable() {
    var empty = ListResult.<String>of(List.of());
    assertEquals(0, empty.size());
    assertTrue(empty.isEmpty());
    assertTrue(empty.first().isEmpty());

    var populated = ListResult.of(List.of("a", "b"));
    assertEquals(2, populated.size());
    assertEquals("a", populated.first().orElseThrow());
    assertEquals(2, populated.stream().count());
    var seen = 0;
    for (var ignored : populated) {
      seen++;
    }
    assertEquals(2, seen);
  }

  @Test
  void helmExceptionCarriesStructuredFailure() {
    var error = new HelmException("boom", "runOperation", "install");
    assertEquals("boom", error.getMessage());
    assertEquals("runOperation", error.stage());
    assertEquals("install", error.operation());
    assertEquals(new HelmFailure("boom", "runOperation", "install"), error.failure());

    var cause = new IllegalStateException("root");
    var wrapped = new HelmException(new HelmFailure("boom", "runOperation", "install"), cause);
    assertSame(cause, wrapped.getCause());

    assertThrows(
        HelmException.class,
        () -> {
          throw error;
        });
  }
}
