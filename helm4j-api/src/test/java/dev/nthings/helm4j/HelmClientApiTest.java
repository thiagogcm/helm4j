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
 * Exercises the public namespace clients and fluent terminal builders against an in-module {@link
 * FakeHelmGateway}, covering the API surface without the native runtime.
 */
@DisplayName("HelmClient API: namespace client delegation and fluent terminal builders")
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

    repo.add().name("bitnami").url("https://charts.bitnami.com/bitnami").execute();
    assertInstanceOf(RepoAddRequest.class, fake.lastRequest);

    repo.update().execute();
    repo.update().names("bitnami").timeout(Duration.ofSeconds(20)).execute();
    assertEquals(0, repo.update().names("bitnami").execute().size());

    assertEquals(0, repo.list().size());
    assertEquals(0, repo.remove().names("bitnami").execute().size());

    repo.registryLogin().hostname("registry.example").username("u").password("p").execute();
    repo.registryLogout().hostname("registry.example").execute();
  }

  @Test
  void chartClientDelegatesEveryOperation() {
    var fake = new FakeHelmGateway();
    var chart = HelmClient.using(fake).chart();
    var chartRef = ChartRef.repo("bitnami/nginx");

    assertEquals(0, chart.searchRepo().keyword("nginx").execute().size());
    assertEquals(0, chart.searchHub().keyword("nginx").execute().size());

    for (var mode : ShowMode.values()) {
      chart.show(mode, chartRef).execute();
      assertEquals(mode.wireValue(), fake.lastShowMode);
    }

    chart.template().releaseName("nginx").chart(chartRef).execute();
    chart.lint().chartPath(Path.of("/tmp/chart")).execute();
    chart.pull().chartReference("bitnami/nginx").execute();
    chart.push().chartReference("/tmp/nginx.tgz").remote("oci://registry.example").execute();
    chart.packageChart().chartPath(Path.of("/tmp/chart")).execute();
    chart.dependency().chartPath(Path.of("/tmp/chart")).execute();
  }

  @Test
  void releaseClientDelegatesEveryOperation() {
    var fake = new FakeHelmGateway();
    var release = HelmClient.using(fake).release();
    var chartRef = ChartRef.repo("bitnami/nginx");

    release.install().releaseName("nginx").chart(chartRef).execute();
    release.upgrade().releaseName("nginx").chart(chartRef).execute();
    release.uninstall().releaseName("nginx").execute();
    release.status().releaseName("nginx").execute();
    release.rollback().releaseName("nginx").revision(1).execute();
    assertEquals(0, release.history().releaseName("nginx").execute().size());

    assertEquals(0, release.list().execute().size());

    release.test().releaseName("nginx").execute();
    release.get().releaseName("nginx").all();
    release.get().releaseName("nginx").values();
    release.get().releaseName("nginx").manifest();
    release.get().releaseName("nginx").hooks();
    release.get().releaseName("nginx").notes();
    release.get().releaseName("nginx").metadata();

    assertEquals(14, fake.invocations);
  }

  @Test
  @DisplayName("A request builder created via builder() is not bound and rejects execute()")
  void unboundBuilderRejectsExecute() {
    var unbound =
        InstallRequest.builder().releaseName("nginx").chart(ChartRef.repo("bitnami/nginx"));
    assertThrows(IllegalStateException.class, unbound::execute);
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
