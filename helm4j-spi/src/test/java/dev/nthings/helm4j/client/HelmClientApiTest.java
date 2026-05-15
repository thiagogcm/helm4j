package dev.nthings.helm4j.client;

import java.nio.file.Path;
import java.time.Duration;

import dev.nthings.helm4j.chart.ChartRef;
import dev.nthings.helm4j.chart.DependencyRequest;
import dev.nthings.helm4j.chart.HubSearchRequest;
import dev.nthings.helm4j.chart.LintRequest;
import dev.nthings.helm4j.chart.PackageChartRequest;
import dev.nthings.helm4j.chart.PullRequest;
import dev.nthings.helm4j.chart.PushRequest;
import dev.nthings.helm4j.chart.RepoSearchRequest;
import dev.nthings.helm4j.chart.ShowMode;
import dev.nthings.helm4j.chart.ShowRequest;
import dev.nthings.helm4j.chart.TemplateRequest;
import dev.nthings.helm4j.release.GetRequest;
import dev.nthings.helm4j.release.HistoryRequest;
import dev.nthings.helm4j.release.InstallRequest;
import dev.nthings.helm4j.release.ReleaseListRequest;
import dev.nthings.helm4j.release.RollbackRequest;
import dev.nthings.helm4j.release.StatusRequest;
import dev.nthings.helm4j.release.TestRequest;
import dev.nthings.helm4j.release.UninstallRequest;
import dev.nthings.helm4j.release.UpgradeRequest;
import dev.nthings.helm4j.repo.RegistryLoginRequest;
import dev.nthings.helm4j.repo.RegistryLogoutRequest;
import dev.nthings.helm4j.repo.RepoAddRequest;
import dev.nthings.helm4j.repo.RepoRemoveRequest;
import dev.nthings.helm4j.repo.RepoUpdateRequest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the public namespace clients and both entry-point styles — the consumer-based fluent
 * builders and the pre-built request overloads — against an in-module {@link FakeHelmGateway},
 * covering the API surface without the native runtime.
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
  @DisplayName("Pre-built requests can be passed to every repo client overload")
  void repoClientAcceptsPreBuiltRequests() {
    var fake = new FakeHelmGateway();
    var repo = HelmClient.using(fake).repo();

    repo.add(RepoAddRequest.builder().name("bitnami").url("https://charts.example.com").build());
    repo.update(RepoUpdateRequest.builder().names("bitnami").build());
    repo.remove(RepoRemoveRequest.builder().names("bitnami").build());
    repo.registryLogin(RegistryLoginRequest.builder().hostname("registry.example").build());
    repo.registryLogout(RegistryLogoutRequest.builder().hostname("registry.example").build());

    assertEquals(5, fake.invocations);
  }

  @Test
  @DisplayName("Pre-built requests can be passed to every chart client overload")
  void chartClientAcceptsPreBuiltRequests() {
    var fake = new FakeHelmGateway();
    var chart = HelmClient.using(fake).chart();
    var chartRef = ChartRef.repo("bitnami/nginx");

    chart.searchRepo(RepoSearchRequest.builder().keyword("nginx").build());
    chart.searchHub(HubSearchRequest.builder().keyword("nginx").build());
    chart.show(ShowMode.CHART, chartRef, ShowRequest.builder().build());
    chart.template(TemplateRequest.builder().releaseName("nginx").chart(chartRef).build());
    chart.lint(LintRequest.builder().chartPath(Path.of("/tmp/chart")).build());
    chart.pull(PullRequest.builder().chart(chartRef).build());
    chart.push(PushRequest.builder().chartReference("/tmp/nginx.tgz").build());
    chart.packageChart(PackageChartRequest.builder().chartPath(Path.of("/tmp/chart")).build());
    chart.dependency(DependencyRequest.builder().chartPath(Path.of("/tmp/chart")).build());

    assertEquals(9, fake.invocations);
  }

  @Test
  @DisplayName("Pre-built requests can be passed to every release client overload")
  void releaseClientAcceptsPreBuiltRequests() {
    var fake = new FakeHelmGateway();
    var release = HelmClient.using(fake).release();
    var chartRef = ChartRef.repo("bitnami/nginx");

    release.install(InstallRequest.builder().releaseName("nginx").chart(chartRef).build());
    release.upgrade(UpgradeRequest.builder().releaseName("nginx").chart(chartRef).build());
    release.uninstall(UninstallRequest.builder().releaseName("nginx").build());
    release.status(StatusRequest.builder().releaseName("nginx").build());
    release.rollback(RollbackRequest.builder().releaseName("nginx").revision(1).build());
    release.history(HistoryRequest.builder().releaseName("nginx").build());
    release.list(ReleaseListRequest.builder().build());
    release.test(TestRequest.builder().releaseName("nginx").build());

    var get = GetRequest.builder().releaseName("nginx").build();
    release.getAll(get);
    release.getValues(get);
    release.getManifest(get);
    release.getHooks(get);
    release.getNotes(get);
    release.getMetadata(get);

    assertEquals(14, fake.invocations);
  }

  @Test
  @DisplayName("A pre-built request reaches the gateway unchanged")
  void preBuiltRequestReachesGatewayUnchanged() {
    var fake = new FakeHelmGateway();
    var release = HelmClient.using(fake).release();
    var request = InstallRequest.builder().releaseName("nginx").chart(ChartRef.repo("a/b")).build();
    release.install(request);
    assertSame(request, fake.lastRequest);
  }

  @Test
  @DisplayName("The static entry points require a discovered HelmGatewayProvider")
  void staticEntryPointsRequireAProvider() {
    // No helm4j-native (or any HelmGatewayProvider) is on the test module path, so gateway
    // discovery fails fast. This exercises HelmClient.create() and the Helm bootstrap facade.
    var fromCreate = assertThrows(IllegalStateException.class, HelmClient::create);
    assertTrue(fromCreate.getMessage().contains("helm4j-native"));
    assertThrows(IllegalStateException.class, Helm::client);
    assertThrows(IllegalStateException.class, Helm::version);
  }
}
