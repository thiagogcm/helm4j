package dev.nthings.helm4j;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.nthings.helm4j.chart.HubChartSummary;
import dev.nthings.helm4j.chart.RepoChartSummary;
import dev.nthings.helm4j.chart.ShowMode;
import dev.nthings.helm4j.errors.HelmException;
import dev.nthings.helm4j.internal.sdk.HelmBridge;
import dev.nthings.helm4j.release.ApplyStrategy;
import dev.nthings.helm4j.release.DryRunMode;
import dev.nthings.helm4j.release.InstallFailure;
import dev.nthings.helm4j.release.InstallPending;
import dev.nthings.helm4j.release.InstallSuccess;
import dev.nthings.helm4j.release.WaitMode;
import dev.nthings.helm4j.repo.RepoAddFailure;
import dev.nthings.helm4j.repo.RepoAddSuccess;
import dev.nthings.helm4j.repo.RepoSummary;
import dev.nthings.helm4j.repo.RepoUpdateEntry;
import dev.nthings.helm4j.types.ChartRef;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
      var add = helm.repo()
          .add(
              spec -> spec.name("bitnami")
                  .url("https://charts.bitnami.com/bitnami")
                  .timeout(Duration.ofSeconds(5))
                  .forceUpdate(true));

      var addSuccess = assertInstanceOf(RepoAddSuccess.class, add);
      assertEquals("bitnami", addSuccess.name());
      assertEquals("https://charts.bitnami.com/bitnami", addSuccess.url());

      var search = helm.chart()
          .searchRepo(
              "nginx",
              spec -> spec.includeAllVersions(true)
                  .includePreReleaseVersions(true)
                  .maxColumnWidth(120));
      assertEquals(1, search.size());
      assertEquals("bitnami/nginx", search.first().orElseThrow().name());

      var install = helm.release()
          .install(
              spec -> spec.releaseName("nginx")
                  .chart(ChartRef.repo("bitnami/nginx"))
                  .source(s -> s.repositoryUrl("https://charts.bitnami.com/bitnami"))
                  .namespace("apps")
                  .createNamespace(true)
                  .timeout(Duration.ofMinutes(3))
                  .values(Map.of("service", Map.of("type", "ClusterIP"))));

      var success = assertInstanceOf(InstallSuccess.class, install);
      assertEquals("nginx", success.release().name());
      assertEquals("deployed", success.release().status());
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
      var update = helm.repo().update(spec -> spec.names("bitnami").timeout(Duration.ofSeconds(20)));
      assertEquals(1, update.size());
      assertEquals("bitnami", update.first().orElseThrow().name());

      var list = helm.repo().list();
      assertEquals(1, list.size());
      assertEquals("bitnami", list.first().orElseThrow().name());

      var remove = helm.repo().remove("bitnami");
      assertEquals(1, remove.size());
      assertEquals("bitnami", remove.first().orElseThrow());

      var hub = helm.chart().searchHub("nginx", spec -> spec.listRepositoryUrl(true));
      assertEquals(1, hub.size());
      assertEquals("nginx", hub.first().orElseThrow().name());

      var chartRef = ChartRef.repo("bitnami/nginx");
      var chart = helm.chart().chart(chartRef);
      assertEquals("apiVersion: v2", chart.metadataYaml());

      var values = helm.chart().values(chartRef);
      assertEquals("service:\n  type: ClusterIP", values.valuesYaml());

      var readme = helm.chart().readme(chartRef);
      assertEquals("# Nginx", readme.readmeText());

      var crds = helm.chart().crds(chartRef);
      assertEquals(1, crds.customResourceDefinitions().size());

      var all = helm.chart().all(chartRef);
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
      var pending = helm.release()
          .install(spec -> spec.releaseName("nginx").chart(ChartRef.repo("bitnami/nginx")));
      assertInstanceOf(InstallPending.class, pending);
    }

    var failedBridge = new StubHelmBridge();
    failedBridge.setInstallFailure("chart not found", "runOperation", "install");

    try (var helm = Helm.client(spec -> spec.withBridge(failedBridge))) {
      var failure = helm.release()
          .install(spec -> spec.releaseName("nginx").chart(ChartRef.repo("bitnami/nginx")));
      var typedFailure = assertInstanceOf(InstallFailure.class, failure);
      assertEquals("chart not found", typedFailure.message());
      assertEquals("runOperation", typedFailure.stage());
    }
  }

  @Test
  void domainFailureAndTransportFailureAreHandledSeparately() {
    var domainFailureBridge = new StubHelmBridge();
    domainFailureBridge.setRepoAddFailure("repository already exists", "runOperation", "repo add");

    try (var helm = Helm.client(spec -> spec.withBridge(domainFailureBridge))) {
      var result = helm.repo().add(spec -> spec.name("bitnami").url("https://charts.bitnami.com/bitnami"));
      var failure = assertInstanceOf(RepoAddFailure.class, result);
      assertEquals("repository already exists", failure.message());
    }

    var transportFailureBridge = new StubHelmBridge();
    transportFailureBridge.setSearchRepoFailure(
        "no repositories configured", "runOperation", "search repo");

    try (var helm = Helm.client(spec -> spec.withBridge(transportFailureBridge))) {
      var error = assertThrows(
          HelmException.class,
          () -> helm.chart().searchRepo("nginx", spec -> spec.failIfNoResults(true)));
      assertEquals("runOperation", error.stage());
      assertEquals("search repo", error.operation());
    }
  }

  @Test
  void fluentInstallBuilderDelegatesToClient() {
    var bridge = new StubHelmBridge();
    bridge.setInstallSuccess("nginx", "apps", 1, "deployed");

    try (var helm = Helm.client(spec -> spec.withBridge(bridge))) {
      var result = Helm.install("bitnami/nginx")
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

      var success = assertInstanceOf(InstallSuccess.class, result);
      assertEquals("nginx", success.release().name());
      assertEquals("deployed", success.release().status());
    }

    assertEquals("nginx", bridge.lastInstallReleaseName);
  }

  @Test
  void fluentInstallBuilderAcceptsChartRef() {
    var bridge = new StubHelmBridge();
    bridge.setInstallSuccess("web", "default", 1, "deployed");

    try (var helm = Helm.client(spec -> spec.withBridge(bridge))) {
      var result = Helm.install(ChartRef.oci("oci://registry-1.docker.io/bitnamicharts/nginx"))
          .releaseName("web")
          .run(helm);

      assertInstanceOf(InstallSuccess.class, result);
    }

    assertEquals("web", bridge.lastInstallReleaseName);
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

    String lastRepoAddName;
    String lastSearchKeyword;
    String lastSearchHubKeyword;
    String lastShowMode;
    String lastInstallReleaseName;
    boolean lastInstallServerSideApply;

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

      this.installResponse = asJsonBytes(Map.of("release", release));
    }

    void setInstallFailure(String message, String stage, String operation) {
      this.installResponse = errorPayload(message, stage, operation);
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
