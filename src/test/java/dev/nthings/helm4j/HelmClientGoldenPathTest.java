package dev.nthings.helm4j;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.nthings.helm4j.chart.ChartRef;
import dev.nthings.helm4j.chart.HubChartSummary;
import dev.nthings.helm4j.chart.LintSeverity;
import dev.nthings.helm4j.chart.RepoChartSummary;
import dev.nthings.helm4j.chart.ShowMode;
import dev.nthings.helm4j.errors.HelmException;
import dev.nthings.helm4j.internal.sdk.HelmBridge;
import dev.nthings.helm4j.release.ApplyStrategy;
import dev.nthings.helm4j.release.DryRunMode;
import dev.nthings.helm4j.release.ReleaseFailure;
import dev.nthings.helm4j.release.ReleasePending;
import dev.nthings.helm4j.release.ReleaseStatus;
import dev.nthings.helm4j.release.ReleaseSuccess;
import dev.nthings.helm4j.release.RollbackSuccess;
import dev.nthings.helm4j.release.UninstallSuccess;
import dev.nthings.helm4j.release.WaitMode;
import dev.nthings.helm4j.repo.RepoAddFailure;
import dev.nthings.helm4j.repo.RepoAddSuccess;
import dev.nthings.helm4j.repo.RepoSummary;
import dev.nthings.helm4j.repo.RepoUpdateEntry;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HelmClientGoldenPathTest {

  @Test
  void goldenPathUsesFluentNamespacesAndReturnsTypedResults() {
    var bridge = new StubHelmBridge();
    bridge.setRepoAddSuccess("bitnami", "https://charts.bitnami.com/bitnami");
    bridge.setSearchRepoSuccess(
        List.of(
            new RepoChartSummary(
                "bitnami/nginx",
                "19.0.0",
                "1.27.0",
                "Nginx chart",
                11,
                "bitnami",
                "https://charts.bitnami.com/bitnami")));
    bridge.setInstallSuccess("nginx", "apps", 3, "deployed");

    try (var helm = Helm.client(spec -> spec.withBridge(bridge))) {
      var add =
          helm.repo()
              .add(
                  spec ->
                      spec.name("bitnami")
                          .url("https://charts.bitnami.com/bitnami")
                          .timeout(Duration.ofSeconds(5))
                          .forceUpdate(true));

      var addSuccess = assertInstanceOf(RepoAddSuccess.class, add);
      assertEquals("bitnami", addSuccess.name());
      assertEquals("https://charts.bitnami.com/bitnami", addSuccess.url());

      var search =
          helm.chart()
              .searchRepo(
                  spec ->
                      spec.keyword("nginx")
                          .includeAllVersions(true)
                          .includePreReleaseVersions(true)
                          .maxColumnWidth(120));
      assertEquals(1, search.size());
      assertEquals("bitnami/nginx", search.first().orElseThrow().name());

      var install =
          helm.release()
              .install(
                  spec ->
                      spec.releaseName("nginx")
                          .chart(ChartRef.repo("bitnami/nginx"))
                          .source(s -> s.repositoryUrl("https://charts.bitnami.com/bitnami"))
                          .namespace("apps")
                          .createNamespace(true)
                          .timeout(Duration.ofMinutes(3))
                          .values(Map.of("service", Map.of("type", "ClusterIP"))));

      var success = assertInstanceOf(ReleaseSuccess.class, install);
      assertEquals("nginx", success.release().name());
      assertEquals(ReleaseStatus.DEPLOYED, success.release().status());
    }

    assertEquals("bitnami", bridge.lastRepoAddName);
    assertEquals("nginx", bridge.lastSearchKeyword);
    assertEquals("nginx", bridge.lastInstallReleaseName);
    assertTrue(bridge.lastInstallServerSideApply);
  }

  @Test
  void repoAndChartExtendedOperationsReturnTypedRecords() {
    var bridge = new StubHelmBridge();
    bridge.setRepoUpdateSuccess(List.of(new RepoUpdateEntry("bitnami", "ok")));
    bridge.setRepoListSuccess(
        List.of(new RepoSummary("bitnami", "https://charts.bitnami.com/bitnami")));
    bridge.setRepoRemoveSuccess(List.of("bitnami"));
    bridge.setSearchHubSuccess(
        List.of(
            new HubChartSummary(
                "nginx",
                "19.0.0",
                "1.27.0",
                "Nginx chart",
                0,
                "https://artifacthub.io/packages/helm/bitnami/nginx",
                "bitnami",
                "https://charts.bitnami.com/bitnami")));
    bridge.setShowSuccess(
        ShowMode.CHART.wireValue(),
        "bitnami/nginx",
        "/tmp/nginx",
        "apiVersion: v2",
        null,
        null,
        List.of(),
        "show chart output");
    bridge.setShowSuccess(
        ShowMode.VALUES.wireValue(),
        "bitnami/nginx",
        "/tmp/nginx",
        null,
        "service:\n  type: ClusterIP",
        null,
        List.of(),
        "show values output");
    bridge.setShowSuccess(
        ShowMode.README.wireValue(),
        "bitnami/nginx",
        "/tmp/nginx",
        null,
        null,
        "# Nginx",
        List.of(),
        "show readme output");
    bridge.setShowSuccess(
        ShowMode.CRDS.wireValue(),
        "bitnami/nginx",
        "/tmp/nginx",
        null,
        null,
        null,
        List.of("kind: CustomResourceDefinition"),
        "show crds output");
    bridge.setShowSuccess(
        ShowMode.ALL.wireValue(),
        "bitnami/nginx",
        "/tmp/nginx",
        "apiVersion: v2",
        "service:\n  type: ClusterIP",
        "# Nginx",
        List.of("kind: CustomResourceDefinition"),
        "show all output");

    try (var helm = Helm.client(spec -> spec.withBridge(bridge))) {
      var update =
          helm.repo().update(spec -> spec.names("bitnami").timeout(Duration.ofSeconds(20)));
      assertEquals(1, update.size());
      assertEquals("bitnami", update.first().orElseThrow().name());

      var list = helm.repo().list();
      assertEquals(1, list.size());
      assertEquals("bitnami", list.first().orElseThrow().name());

      var remove = helm.repo().remove(spec -> spec.names("bitnami"));
      assertEquals(1, remove.size());
      assertEquals("bitnami", remove.first().orElseThrow());

      var hub = helm.chart().searchHub(spec -> spec.keyword("nginx").listRepositoryUrl(true));
      assertEquals(1, hub.size());
      assertEquals("nginx", hub.first().orElseThrow().name());

      var chartRef = ChartRef.repo("bitnami/nginx");
      var chart = helm.chart().show(ShowMode.CHART, chartRef, spec -> {});
      assertEquals("apiVersion: v2", chart.metadataYaml());

      var values = helm.chart().show(ShowMode.VALUES, chartRef, spec -> {});
      assertEquals("service:\n  type: ClusterIP", values.valuesYaml());

      var readme = helm.chart().show(ShowMode.README, chartRef, spec -> {});
      assertEquals("# Nginx", readme.readmeText());

      var crds = helm.chart().show(ShowMode.CRDS, chartRef, spec -> {});
      assertEquals(1, crds.customResourceDefinitions().size());

      var all = helm.chart().show(ShowMode.ALL, chartRef, spec -> {});
      assertEquals("apiVersion: v2", all.metadataYaml());
      assertEquals("# Nginx", all.readmeText());
      assertEquals(1, all.customResourceDefinitions().size());
    }

    assertEquals("nginx", bridge.lastSearchHubKeyword);
    assertEquals("all", bridge.lastShowMode);
  }

  @Test
  void pendingAndFailureInstallOutcomesMapToSealedResults() {
    var pendingBridge = new StubHelmBridge();
    pendingBridge.setInstallSuccess("nginx", "apps", 1, "pending-install");

    try (var helm = Helm.client(spec -> spec.withBridge(pendingBridge))) {
      var pending =
          helm.release()
              .install(spec -> spec.releaseName("nginx").chart(ChartRef.repo("bitnami/nginx")));
      assertInstanceOf(ReleasePending.class, pending);
    }

    var failedBridge = new StubHelmBridge();
    failedBridge.setInstallFailure("chart not found", "runOperation", "install");

    try (var helm = Helm.client(spec -> spec.withBridge(failedBridge))) {
      var failure =
          helm.release()
              .install(spec -> spec.releaseName("nginx").chart(ChartRef.repo("bitnami/nginx")));
      var typedFailure = assertInstanceOf(ReleaseFailure.class, failure);
      assertEquals("chart not found", typedFailure.message());
      assertEquals("runOperation", typedFailure.stage());
    }
  }

  @Test
  void domainFailureAndTransportFailureAreHandledSeparately() {
    var domainFailureBridge = new StubHelmBridge();
    domainFailureBridge.setRepoAddFailure("repository already exists", "runOperation", "repo add");

    try (var helm = Helm.client(spec -> spec.withBridge(domainFailureBridge))) {
      var result =
          helm.repo().add(spec -> spec.name("bitnami").url("https://charts.bitnami.com/bitnami"));
      var failure = assertInstanceOf(RepoAddFailure.class, result);
      assertEquals("repository already exists", failure.message());
    }

    var transportFailureBridge = new StubHelmBridge();
    transportFailureBridge.setSearchRepoFailure(
        "no repositories configured", "runOperation", "search repo");

    try (var helm = Helm.client(spec -> spec.withBridge(transportFailureBridge))) {
      var error =
          assertThrows(
              HelmException.class,
              () -> helm.chart().searchRepo(spec -> spec.keyword("nginx").failIfNoResults(true)));
      assertEquals("runOperation", error.stage());
      assertEquals("search repo", error.operation());
    }
  }

  @Test
  void fluentInstallBuilderDelegatesToClient() {
    var bridge = new StubHelmBridge();
    bridge.setInstallSuccess("nginx", "apps", 1, "deployed");

    try (var helm = Helm.client(spec -> spec.withBridge(bridge))) {
      var result =
          Helm.install(ChartRef.repo("bitnami/nginx"))
              .releaseName("nginx")
              .version("19.0.0")
              .namespace("apps")
              .createNamespace(true)
              .dryRun(DryRunMode.NONE)
              .waitMode(WaitMode.WATCHER)
              .timeout(Duration.ofMinutes(3))
              .applyStrategy(ApplyStrategy.SERVER_SIDE_APPLY_FORCE_CONFLICTS)
              .values(Map.of("service", Map.of("type", "ClusterIP")))
              .labels(Map.of("team", "platform"))
              .run(helm);

      var success = assertInstanceOf(ReleaseSuccess.class, result);
      assertEquals("nginx", success.release().name());
      assertEquals(ReleaseStatus.DEPLOYED, success.release().status());
    }

    assertEquals("nginx", bridge.lastInstallReleaseName);
  }

  @Test
  void fluentInstallBuilderAcceptsChartRef() {
    var bridge = new StubHelmBridge();
    bridge.setInstallSuccess("web", "default", 1, "deployed");

    try (var helm = Helm.client(spec -> spec.withBridge(bridge))) {
      var result =
          Helm.install(ChartRef.oci("oci://registry-1.docker.io/bitnamicharts/nginx"))
              .releaseName("web")
              .run(helm);

      assertInstanceOf(ReleaseSuccess.class, result);
    }

    assertEquals("web", bridge.lastInstallReleaseName);
  }

  @Test
  void upgradeReturnsTypedResult() {
    var bridge = new StubHelmBridge();
    bridge.setUpgradeSuccess("nginx", "apps", 2, "deployed");

    try (var helm = Helm.client(spec -> spec.withBridge(bridge))) {
      var result =
          helm.release()
              .upgrade(
                  spec ->
                      spec.releaseName("nginx")
                          .chart(ChartRef.repo("bitnami/nginx"))
                          .namespace("apps")
                          .install(true)
                          .reuseValues(true)
                          .values(Map.of("replicaCount", 3)));

      var success = assertInstanceOf(ReleaseSuccess.class, result);
      assertEquals("nginx", success.release().name());
      assertEquals(2, success.release().revision());
      assertEquals(ReleaseStatus.DEPLOYED, success.release().status());
    }

    assertEquals("nginx", bridge.lastUpgradeReleaseName);
  }

  @Test
  void upgradePendingStatusMapsToPendingResult() {
    var bridge = new StubHelmBridge();
    bridge.setUpgradeSuccess("nginx", "apps", 2, "pending-upgrade");

    try (var helm = Helm.client(spec -> spec.withBridge(bridge))) {
      var result =
          helm.release()
              .upgrade(spec -> spec.releaseName("nginx").chart(ChartRef.repo("bitnami/nginx")));
      assertInstanceOf(ReleasePending.class, result);
    }
  }

  @Test
  void fluentUpgradeBuilderDelegatesToClient() {
    var bridge = new StubHelmBridge();
    bridge.setUpgradeSuccess("nginx", "apps", 3, "deployed");

    try (var helm = Helm.client(spec -> spec.withBridge(bridge))) {
      var result =
          Helm.upgrade(ChartRef.repo("bitnami/nginx"))
              .releaseName("nginx")
              .namespace("apps")
              .install(true)
              .values(Map.of("replicaCount", 3))
              .run(helm);

      var success = assertInstanceOf(ReleaseSuccess.class, result);
      assertEquals("nginx", success.release().name());
    }

    assertEquals("nginx", bridge.lastUpgradeReleaseName);
  }

  @Test
  void uninstallReturnsTypedResult() {
    var bridge = new StubHelmBridge();
    bridge.setUninstallSuccess("release \"my-release\" uninstalled");

    try (var helm = Helm.client(spec -> spec.withBridge(bridge))) {
      var result = helm.release().uninstall(spec -> spec.releaseName("my-release"));

      var success = assertInstanceOf(UninstallSuccess.class, result);
      assertEquals("release \"my-release\" uninstalled", success.info());
    }

    assertEquals("my-release", bridge.lastUninstallReleaseName);
  }

  @Test
  void statusReturnsReleaseInfo() {
    var bridge = new StubHelmBridge();
    bridge.setStatusSuccess("nginx", "apps", 2, "deployed");

    try (var helm = Helm.client(spec -> spec.withBridge(bridge))) {
      var result = helm.release().status(spec -> spec.releaseName("nginx"));
      assertEquals("nginx", result.release().name());
      assertEquals(ReleaseStatus.DEPLOYED, result.release().status());
    }

    assertEquals("nginx", bridge.lastStatusReleaseName);
  }

  @Test
  void rollbackReturnsSuccessResult() {
    var bridge = new StubHelmBridge();
    bridge.setRollbackSuccess("nginx", 1);

    try (var helm = Helm.client(spec -> spec.withBridge(bridge))) {
      var result = helm.release().rollback(spec -> spec.releaseName("nginx").revision(1));

      var success = assertInstanceOf(RollbackSuccess.class, result);
      assertEquals("nginx", success.releaseName());
      assertEquals(1, success.revision());
    }

    assertEquals("nginx", bridge.lastRollbackReleaseName);
  }

  @Test
  void historyReturnsEntries() {
    var bridge = new StubHelmBridge();
    bridge.setHistorySuccess(
        List.of(
            Map.of(
                "revision",
                1,
                "updated",
                "2026-01-01T00:00:00Z",
                "status",
                "deployed",
                "chart",
                "nginx",
                "chartVersion",
                "19.0.0",
                "appVersion",
                "1.27.0",
                "description",
                "install")));

    try (var helm = Helm.client(spec -> spec.withBridge(bridge))) {
      var result = helm.release().history(spec -> spec.releaseName("nginx"));
      assertEquals(1, result.size());
      assertEquals(1, result.first().orElseThrow().revision());
      assertEquals(ReleaseStatus.DEPLOYED, result.first().orElseThrow().status());
    }

    assertEquals("nginx", bridge.lastHistoryReleaseName);
  }

  @Test
  void getValuesReturnsMap() {
    var bridge = new StubHelmBridge();
    bridge.setGetValuesSuccess(Map.of("replicaCount", 3, "image", Map.of("tag", "latest")));

    try (var helm = Helm.client(spec -> spec.withBridge(bridge))) {
      var result = helm.release().getValues(spec -> spec.releaseName("nginx"));
      assertEquals(3, result.values().get("replicaCount"));
    }

    assertEquals("values", bridge.lastGetMode);
    assertEquals("nginx", bridge.lastGetReleaseName);
  }

  @Test
  void getOperationsReturnTypedResults() {
    var bridge = new StubHelmBridge();
    var hookPayload =
        Map.of(
            "name",
            "pre-install",
            "kind",
            "Job",
            "path",
            "templates/hook.yaml",
            "events",
            List.of("pre-install"),
            "weight",
            1);
    bridge.setGetAllSuccess(
        StubHelmBridge.releaseMap("nginx", "apps", 2, "deployed"),
        Map.of("replicaCount", 3),
        "---\nkind: ConfigMap",
        List.of(hookPayload),
        "release notes");
    bridge.setGetValuesSuccess(Map.of("replicaCount", 3));
    bridge.setGetManifestSuccess("---\nkind: Secret");
    bridge.setGetHooksSuccess(List.of(hookPayload));
    bridge.setGetNotesSuccess("note text");
    bridge.setGetMetadataSuccess(
        "nginx", "apps", 2, "deployed", "nginx", "19.0.0", "1.27.0", "2026-01-01T00:00:00Z");

    try (var helm = Helm.client(spec -> spec.withBridge(bridge))) {
      var all = helm.release().getAll(spec -> spec.releaseName("nginx"));
      assertEquals("nginx", all.release().name());
      assertEquals(1, all.hooks().size());
      assertEquals("all", bridge.lastGetMode);

      var values = helm.release().getValues(spec -> spec.releaseName("nginx"));
      assertEquals(3, values.values().get("replicaCount"));
      assertEquals("values", bridge.lastGetMode);

      var manifest = helm.release().getManifest(spec -> spec.releaseName("nginx"));
      assertTrue(manifest.manifest().contains("kind: Secret"));
      assertEquals("manifest", bridge.lastGetMode);

      var hooks = helm.release().getHooks(spec -> spec.releaseName("nginx"));
      assertEquals("pre-install", hooks.hooks().getFirst().name());
      assertEquals("hooks", bridge.lastGetMode);

      var notes = helm.release().getNotes(spec -> spec.releaseName("nginx"));
      assertEquals("note text", notes.notes());
      assertEquals("notes", bridge.lastGetMode);

      var metadata = helm.release().getMetadata(spec -> spec.releaseName("nginx"));
      assertEquals("nginx", metadata.name());
      assertEquals("metadata", bridge.lastGetMode);
    }

    assertEquals("nginx", bridge.lastGetReleaseName);
  }

  @Test
  void upgradeUninstallAndRollbackFailuresMapToTypedFailures() {
    var bridge = new StubHelmBridge();
    bridge.setUpgradeFailure("cannot upgrade", "runOperation", "upgrade");
    bridge.setUninstallFailure("cannot uninstall", "runOperation", "uninstall");
    bridge.setRollbackFailure("cannot rollback", "runOperation", "rollback");

    try (var helm = Helm.client(spec -> spec.withBridge(bridge))) {
      var upgrade =
          helm.release()
              .upgrade(spec -> spec.releaseName("nginx").chart(ChartRef.repo("bitnami/nginx")));
      var upgradeFailure = assertInstanceOf(ReleaseFailure.class, upgrade);
      assertEquals("cannot upgrade", upgradeFailure.message());

      var uninstall = helm.release().uninstall(spec -> spec.releaseName("nginx"));
      var uninstallFailure = assertInstanceOf(ReleaseFailure.class, uninstall);
      assertEquals("cannot uninstall", uninstallFailure.message());

      var rollback = helm.release().rollback(spec -> spec.releaseName("nginx"));
      var rollbackFailure = assertInstanceOf(ReleaseFailure.class, rollback);
      assertEquals("cannot rollback", rollbackFailure.message());
    }
  }

  @Test
  void templateReturnsRenderedManifest() {
    var bridge = new StubHelmBridge();
    bridge.setTemplateSuccess("nginx", "default", 1, "---\napiVersion: v1\nkind: Service");

    try (var helm = Helm.client(spec -> spec.withBridge(bridge))) {
      var result =
          helm.chart()
              .template(spec -> spec.releaseName("nginx").chart(ChartRef.repo("bitnami/nginx")));
      assertEquals("nginx", result.release().name());
      assertTrue(result.manifest().contains("apiVersion: v1"));
    }

    assertEquals("nginx", bridge.lastTemplateReleaseName);
  }

  @Test
  void lintReturnsResult() {
    var bridge = new StubHelmBridge();
    bridge.setLintSuccess(1, 1, 0);

    try (var helm = Helm.client(spec -> spec.withBridge(bridge))) {
      var result = helm.chart().lint(spec -> spec.chartPath(java.nio.file.Path.of("/tmp/chart")));
      assertTrue(result.passed());
      assertEquals(1, result.totalCharts());
      assertEquals(0, result.chartsFailed());
    }

    assertTrue(bridge.lastLintChartPath.endsWith("/tmp/chart"));
  }

  @Test
  void lintSeverityIsParsedFromWireValues() {
    var bridge = new StubHelmBridge();
    bridge.setLintSuccess(
        List.of(
            Map.of("severity", "INFO", "message", "ok"),
            Map.of("severity", "WARNING", "message", "warn"),
            Map.of("severity", "ERROR", "message", "err"),
            Map.of("severity", "something-else", "message", "unknown")),
        4,
        4,
        1);

    try (var helm = Helm.client(spec -> spec.withBridge(bridge))) {
      var result = helm.chart().lint(spec -> spec.chartPath(java.nio.file.Path.of("/tmp/chart")));
      assertFalse(result.passed());
      assertEquals(LintSeverity.INFO, result.messages().get(0).severity());
      assertEquals(LintSeverity.WARNING, result.messages().get(1).severity());
      assertEquals(LintSeverity.ERROR, result.messages().get(2).severity());
      assertEquals(LintSeverity.UNKNOWN, result.messages().get(3).severity());
    }
  }

  @Test
  void versionReturnsInfo() {
    var bridge = new StubHelmBridge();
    bridge.setVersionSuccess("0.1.0", "go1.26", "v4.1.1");

    try (var helm = Helm.client(spec -> spec.withBridge(bridge))) {
      var info = helm.version();
      assertEquals("0.1.0", info.version());
      assertEquals("go1.26", info.goVersion());
      assertEquals("v4.1.1", info.helmVersion());
    }
  }

  @Test
  void clientBuilderAcceptsCustomObjectMapper() {
    var bridge = new StubHelmBridge();
    bridge.setVersionSuccess("0.2.0", "go1.26", "v4.1.2");

    try (var helm =
        Helm.client(
            spec -> spec.withBridge(bridge).withObjectMapper(JsonMapper.builder().build()))) {
      var info = helm.version();
      assertEquals("0.2.0", info.version());
      assertEquals("v4.1.2", info.helmVersion());
    }
  }

  @Test
  void extendedOperationsAreExposedThroughSdkNamespaces() {
    var bridge = new StubHelmBridge();
    bridge.setListSuccess(List.of(StubHelmBridge.releaseMap("nginx", "apps", 2, "deployed")));
    bridge.setPullSuccess("Pulled: bitnami/nginx");
    bridge.setPushSuccess("Pushed: oci://registry.example/charts/nginx");
    bridge.setPackageSuccess("/tmp/nginx-19.0.0.tgz");
    bridge.setDependencySuccess("NAME\tVERSION\ncommon\t2.2.0");
    bridge.setRegistrySuccess("registry.example", "ok");
    bridge.setTestSuccess(
        StubHelmBridge.releaseMap("nginx", "apps", 2, "deployed"),
        List.of(Map.of("name", "nginx-test", "status", "succeeded")));

    try (var helm = Helm.client(spec -> spec.withBridge(bridge))) {
      var listed = helm.release().list();
      assertEquals(1, listed.size());
      assertEquals("nginx", listed.first().orElseThrow().name());

      var pull = helm.chart().pull(spec -> spec.chartReference("bitnami/nginx"));
      assertTrue(pull.output().contains("Pulled"));

      var push =
          helm.chart()
              .push(
                  spec ->
                      spec.chartReference("/tmp/nginx-19.0.0.tgz")
                          .remote("oci://registry.example/charts"));
      assertTrue(push.output().contains("Pushed"));

      var pkg = helm.chart().packageChart(spec -> spec.chartPath(Path.of("/tmp/chart")));
      assertEquals("/tmp/nginx-19.0.0.tgz", pkg.path());

      var deps = helm.chart().dependency(spec -> spec.chartPath(Path.of("/tmp/chart")));
      assertTrue(deps.output().contains("common"));

      var login =
          helm.repo()
              .registryLogin(
                  spec -> spec.hostname("registry.example").username("user").password("secret"));
      assertEquals("registry.example", login.hostname());

      var logout = helm.repo().registryLogout(spec -> spec.hostname("registry.example"));
      assertEquals("ok", logout.status());

      var test = helm.release().test(spec -> spec.releaseName("nginx"));
      assertEquals("nginx", test.release().name());
      assertEquals(1, test.results().size());
      assertEquals("nginx-test", test.results().getFirst().name());
    }
  }

  static final class StubHelmBridge implements HelmBridge {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private byte[] repoAddResponse = utf8("{}");
    private byte[] repoUpdateResponse = utf8("{}");
    private byte[] repoListResponse = utf8("{}");
    private byte[] repoRemoveResponse = utf8("{}");
    private byte[] searchRepoResponse = utf8("{}");
    private byte[] searchHubResponse = utf8("{}");
    private final Map<String, byte[]> showResponses = new HashMap<>();
    private byte[] installResponse = utf8("{}");
    private byte[] upgradeResponse = utf8("{}");
    private byte[] uninstallResponse = utf8("{}");
    private byte[] statusResponse = utf8("{}");
    private byte[] rollbackResponse = utf8("{}");
    private byte[] historyResponse = utf8("{}");
    private byte[] getResponse = utf8("{}");
    private final Map<String, byte[]> getResponses = new HashMap<>();
    private byte[] listResponse = utf8("{}");
    private byte[] pullResponse = utf8("{}");
    private byte[] pushResponse = utf8("{}");
    private byte[] packageResponse = utf8("{}");
    private byte[] dependencyResponse = utf8("{}");
    private byte[] registryResponse = utf8("{}");
    private byte[] testResponse = utf8("{}");
    private byte[] templateResponse = utf8("{}");
    private byte[] lintResponse = utf8("{}");
    private byte[] versionResponse = utf8("{}");

    String lastRepoAddName;
    String lastSearchKeyword;
    String lastSearchHubKeyword;
    String lastShowMode;
    String lastInstallReleaseName;
    boolean lastInstallServerSideApply;
    String lastUpgradeReleaseName;
    String lastUninstallReleaseName;
    String lastStatusReleaseName;
    String lastRollbackReleaseName;
    String lastHistoryReleaseName;
    String lastGetMode;
    String lastGetReleaseName;
    String lastTemplateReleaseName;
    String lastLintChartPath;

    void setRepoAddSuccess(String name, String url) {
      this.repoAddResponse = asJsonBytes(Map.of("name", name, "url", url));
    }

    void setRepoAddFailure(String message, String stage, String operation) {
      this.repoAddResponse = errorPayload(message, stage, operation);
    }

    void setRepoUpdateSuccess(List<RepoUpdateEntry> entries) {
      this.repoUpdateResponse = asJsonBytes(Map.of("repositories", entries));
    }

    void setRepoListSuccess(List<RepoSummary> entries) {
      this.repoListResponse = asJsonBytes(Map.of("repositories", entries));
    }

    void setRepoRemoveSuccess(List<String> names) {
      this.repoRemoveResponse = asJsonBytes(Map.of("removed", names));
    }

    void setSearchRepoSuccess(List<RepoChartSummary> charts) {
      this.searchRepoResponse = asJsonBytes(Map.of("mode", "repo", "results", charts));
    }

    void setSearchRepoFailure(String message, String stage, String operation) {
      this.searchRepoResponse = errorPayload(message, stage, operation);
    }

    void setSearchHubSuccess(List<HubChartSummary> charts) {
      this.searchHubResponse = asJsonBytes(Map.of("mode", "hub", "results", charts));
    }

    void setShowSuccess(
        String mode,
        String chartReference,
        String chartPath,
        String chartText,
        String valuesText,
        String readmeText,
        List<String> crds,
        String rawOutput) {
      var sections = new LinkedHashMap<String, Object>();
      sections.put("chart", chartText);
      sections.put("values", valuesText);
      sections.put("readme", readmeText);
      sections.put("crds", crds);

      var payload = new LinkedHashMap<String, Object>();
      payload.put("mode", mode);
      payload.put("chartRef", chartReference);
      payload.put("chartPath", chartPath);
      payload.put("sections", sections);
      payload.put("cliOutput", rawOutput);

      showResponses.put(mode, asJsonBytes(payload));
    }

    void setInstallSuccess(String name, String namespace, int revision, String status) {
      this.installResponse =
          asJsonBytes(Map.of("release", releaseMap(name, namespace, revision, status)));
    }

    void setInstallFailure(String message, String stage, String operation) {
      this.installResponse = errorPayload(message, stage, operation);
    }

    void setUpgradeSuccess(String name, String namespace, int revision, String status) {
      this.upgradeResponse =
          asJsonBytes(Map.of("release", releaseMap(name, namespace, revision, status)));
    }

    void setUpgradeFailure(String message, String stage, String operation) {
      this.upgradeResponse = errorPayload(message, stage, operation);
    }

    void setUninstallSuccess(String info) {
      var release = releaseMap("my-release", "default", 1, "uninstalled");
      this.uninstallResponse = asJsonBytes(Map.of("release", release, "info", info));
    }

    void setUninstallFailure(String message, String stage, String operation) {
      this.uninstallResponse = errorPayload(message, stage, operation);
    }

    void setStatusSuccess(String name, String namespace, int revision, String status) {
      this.statusResponse =
          asJsonBytes(Map.of("release", releaseMap(name, namespace, revision, status)));
    }

    void setRollbackSuccess(String releaseName, int revision) {
      this.rollbackResponse = asJsonBytes(Map.of("releaseName", releaseName, "revision", revision));
    }

    void setRollbackFailure(String message, String stage, String operation) {
      this.rollbackResponse = errorPayload(message, stage, operation);
    }

    void setHistorySuccess(List<Map<String, Object>> entries) {
      this.historyResponse = asJsonBytes(Map.of("entries", entries));
    }

    void setGetValuesSuccess(Map<String, Object> values) {
      this.getResponses.put("values", asJsonBytes(Map.of("values", values)));
    }

    void setGetAllSuccess(
        Map<String, Object> release,
        Map<String, Object> values,
        String manifest,
        List<Map<String, Object>> hooks,
        String notes) {
      this.getResponses.put(
          "all",
          asJsonBytes(
              Map.of(
                  "release",
                  release,
                  "values",
                  values,
                  "manifest",
                  manifest,
                  "hooks",
                  hooks,
                  "notes",
                  notes)));
    }

    void setGetManifestSuccess(String manifest) {
      this.getResponses.put("manifest", asJsonBytes(Map.of("manifest", manifest)));
    }

    void setGetHooksSuccess(List<Map<String, Object>> hooks) {
      this.getResponses.put("hooks", asJsonBytes(Map.of("hooks", hooks)));
    }

    void setGetNotesSuccess(String notes) {
      this.getResponses.put("notes", asJsonBytes(Map.of("notes", notes)));
    }

    void setGetMetadataSuccess(
        String name,
        String namespace,
        int revision,
        String status,
        String chart,
        String chartVersion,
        String appVersion,
        String deployedAt) {
      this.getResponses.put(
          "metadata",
          asJsonBytes(
              Map.of(
                  "name",
                  name,
                  "namespace",
                  namespace,
                  "revision",
                  revision,
                  "status",
                  status,
                  "chart",
                  chart,
                  "chartVersion",
                  chartVersion,
                  "appVersion",
                  appVersion,
                  "deployedAt",
                  deployedAt)));
    }

    void setTemplateSuccess(String name, String namespace, int revision, String manifest) {
      this.templateResponse =
          asJsonBytes(
              Map.of(
                  "release",
                  releaseMap(name, namespace, revision, "deployed"),
                  "manifest",
                  manifest));
    }

    void setLintSuccess(int totalCharts, int chartsTested, int chartsFailed) {
      setLintSuccess(List.of(), totalCharts, chartsTested, chartsFailed);
    }

    void setLintSuccess(
        List<Map<String, String>> messages, int totalCharts, int chartsTested, int chartsFailed) {
      this.lintResponse =
          asJsonBytes(
              Map.of(
                  "messages", messages,
                  "totalCharts", totalCharts,
                  "chartsTested", chartsTested,
                  "chartsFailed", chartsFailed));
    }

    void setVersionSuccess(String version, String goVersion, String helmVersion) {
      this.versionResponse =
          asJsonBytes(
              Map.of("version", version, "goVersion", goVersion, "helmVersion", helmVersion));
    }

    void setListSuccess(List<Map<String, Object>> releases) {
      this.listResponse = asJsonBytes(Map.of("releases", releases));
    }

    void setPullSuccess(String output) {
      this.pullResponse = asJsonBytes(Map.of("output", output));
    }

    void setPushSuccess(String output) {
      this.pushResponse = asJsonBytes(Map.of("output", output));
    }

    void setPackageSuccess(String path) {
      this.packageResponse = asJsonBytes(Map.of("path", path));
    }

    void setDependencySuccess(String output) {
      this.dependencyResponse = asJsonBytes(Map.of("output", output));
    }

    void setRegistrySuccess(String hostname, String status) {
      this.registryResponse = asJsonBytes(Map.of("hostname", hostname, "status", status));
    }

    void setTestSuccess(Map<String, Object> release, List<Map<String, String>> results) {
      this.testResponse = asJsonBytes(Map.of("release", release, "results", results));
    }

    private static Map<String, Object> releaseMap(
        String name, String namespace, int revision, String status) {
      var release = new LinkedHashMap<String, Object>();
      release.put("name", name);
      release.put("namespace", namespace);
      release.put("revision", revision);
      release.put("status", status);
      release.put("description", "desc");
      release.put("firstDeployed", "2026-01-01T00:00:00Z");
      release.put("lastDeployed", "2026-01-01T00:00:00Z");
      release.put("chartName", "nginx");
      release.put("chartVersion", "19.0.0");
      release.put("appVersion", "1.27.0");
      release.put("notes", "notes");
      return release;
    }

    @Override
    public byte[] repo(byte[] mode, byte[] optionsJson) {
      var modeStr = fromUtf8(mode);
      var options = parseJson(optionsJson);
      if ("add".equals(modeStr)) {
        this.lastRepoAddName = text(options, "name");
      }
      return switch (modeStr) {
        case "add" -> repoAddResponse;
        case "update" -> repoUpdateResponse;
        case "list" -> repoListResponse;
        case "remove" -> repoRemoveResponse;
        default -> throw new IllegalStateException("unexpected repo mode: " + modeStr);
      };
    }

    @Override
    public byte[] search(byte[] mode, byte[] optionsJson) {
      var modeStr = fromUtf8(mode);
      var options = parseJson(optionsJson);
      if ("repo".equals(modeStr)) {
        this.lastSearchKeyword = text(options, "keyword");
      } else if ("hub".equals(modeStr)) {
        this.lastSearchHubKeyword = text(options, "keyword");
      }

      return switch (modeStr) {
        case "repo" -> searchRepoResponse;
        case "hub" -> searchHubResponse;
        default -> throw new IllegalStateException("unexpected search mode: " + modeStr);
      };
    }

    @Override
    public byte[] show(byte[] mode, byte[] chartRef, byte[] optionsJson) {
      var modeStr = fromUtf8(mode);
      this.lastShowMode = modeStr;
      return showResponses.get(modeStr);
    }

    @Override
    public byte[] install(byte[] releaseName, byte[] chartRef, byte[] optionsJson) {
      this.lastInstallReleaseName = fromUtf8(releaseName);
      var options = parseJson(optionsJson);
      this.lastInstallServerSideApply = options.path("serverSideApply").asBoolean(false);
      return installResponse;
    }

    @Override
    public byte[] upgrade(byte[] releaseName, byte[] chartRef, byte[] optionsJson) {
      this.lastUpgradeReleaseName = fromUtf8(releaseName);
      return upgradeResponse;
    }

    @Override
    public byte[] uninstall(byte[] releaseName, byte[] optionsJson) {
      this.lastUninstallReleaseName = fromUtf8(releaseName);
      return uninstallResponse;
    }

    @Override
    public byte[] status(byte[] releaseName, byte[] optionsJson) {
      this.lastStatusReleaseName = fromUtf8(releaseName);
      return statusResponse;
    }

    @Override
    public byte[] rollback(byte[] releaseName, byte[] optionsJson) {
      this.lastRollbackReleaseName = fromUtf8(releaseName);
      return rollbackResponse;
    }

    @Override
    public byte[] history(byte[] releaseName, byte[] optionsJson) {
      this.lastHistoryReleaseName = fromUtf8(releaseName);
      return historyResponse;
    }

    @Override
    public byte[] get(byte[] mode, byte[] releaseName, byte[] optionsJson) {
      this.lastGetMode = fromUtf8(mode);
      this.lastGetReleaseName = fromUtf8(releaseName);
      return getResponses.getOrDefault(lastGetMode, getResponse);
    }

    @Override
    public byte[] list(byte[] optionsJson) {
      return listResponse;
    }

    @Override
    public byte[] pull(byte[] chartRef, byte[] optionsJson) {
      return pullResponse;
    }

    @Override
    public byte[] push(byte[] chartRef, byte[] remote, byte[] optionsJson) {
      return pushResponse;
    }

    @Override
    public byte[] packageChart(byte[] chartPath, byte[] optionsJson) {
      return packageResponse;
    }

    @Override
    public byte[] dependency(byte[] chartPath, byte[] optionsJson) {
      return dependencyResponse;
    }

    @Override
    public byte[] registry(byte[] mode, byte[] hostname, byte[] optionsJson) {
      return registryResponse;
    }

    @Override
    public byte[] test(byte[] releaseName, byte[] optionsJson) {
      return testResponse;
    }

    @Override
    public byte[] template(byte[] releaseName, byte[] chartRef, byte[] optionsJson) {
      this.lastTemplateReleaseName = fromUtf8(releaseName);
      return templateResponse;
    }

    @Override
    public byte[] lint(byte[] chartPath, byte[] optionsJson) {
      this.lastLintChartPath = fromUtf8(chartPath);
      return lintResponse;
    }

    @Override
    public byte[] version() {
      return versionResponse;
    }

    private static byte[] errorPayload(String message, String stage, String operation) {
      var payload = new LinkedHashMap<String, String>();
      payload.put("error", message);
      payload.put("stage", stage);
      payload.put("operation", operation);
      return asJsonBytes(payload);
    }

    private static byte[] asJsonBytes(Object value) {
      try {
        return MAPPER.writeValueAsBytes(value);
      } catch (JacksonException error) {
        throw new IllegalStateException("failed to build test JSON", error);
      }
    }

    private static byte[] utf8(String value) {
      return value.getBytes(StandardCharsets.UTF_8);
    }

    private static String fromUtf8(byte[] value) {
      if (value == null) {
        return null;
      }
      return new String(value, StandardCharsets.UTF_8);
    }

    private static JsonNode parseJson(byte[] value) {
      try {
        return MAPPER.readTree(value == null || value.length == 0 ? utf8("{}") : value);
      } catch (JacksonException error) {
        throw new IllegalStateException("failed to parse test JSON", error);
      }
    }

    private static String text(JsonNode node, String field) {
      var value = node.get(field);
      if (value == null || value.isNull()) {
        return null;
      }
      return value.asString();
    }
  }
}
