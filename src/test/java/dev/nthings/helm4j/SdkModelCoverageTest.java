package dev.nthings.helm4j;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import dev.nthings.helm4j.chart.DependencyRequest;
import dev.nthings.helm4j.chart.HubSearchRequest;
import dev.nthings.helm4j.chart.LintMessage;
import dev.nthings.helm4j.chart.LintRequest;
import dev.nthings.helm4j.chart.LintSeverity;
import dev.nthings.helm4j.chart.PackageChartRequest;
import dev.nthings.helm4j.chart.PullRequest;
import dev.nthings.helm4j.chart.PushRequest;
import dev.nthings.helm4j.chart.RepoSearchRequest;
import dev.nthings.helm4j.chart.ShowRequest;
import dev.nthings.helm4j.chart.TemplateRequest;
import dev.nthings.helm4j.errors.HelmException;
import dev.nthings.helm4j.release.ApplyStrategy;
import dev.nthings.helm4j.release.DryRunMode;
import dev.nthings.helm4j.release.GetAllResult;
import dev.nthings.helm4j.release.GetHooksResult;
import dev.nthings.helm4j.release.GetManifestResult;
import dev.nthings.helm4j.release.GetMetadataResult;
import dev.nthings.helm4j.release.GetNotesResult;
import dev.nthings.helm4j.release.GetRequest;
import dev.nthings.helm4j.release.HistoryRequest;
import dev.nthings.helm4j.release.HookInfo;
import dev.nthings.helm4j.release.InstallRequest;
import dev.nthings.helm4j.release.InstallResult;
import dev.nthings.helm4j.release.ReleaseInfo;
import dev.nthings.helm4j.release.ReleaseListRequest;
import dev.nthings.helm4j.release.RollbackFailure;
import dev.nthings.helm4j.release.RollbackRequest;
import dev.nthings.helm4j.release.StatusRequest;
import dev.nthings.helm4j.release.TestRequest;
import dev.nthings.helm4j.release.UninstallFailure;
import dev.nthings.helm4j.release.UninstallRequest;
import dev.nthings.helm4j.release.UpgradeFailure;
import dev.nthings.helm4j.release.UpgradeRequest;
import dev.nthings.helm4j.release.WaitMode;
import dev.nthings.helm4j.repo.RegistryLoginRequest;
import dev.nthings.helm4j.repo.RepoAddRequest;
import dev.nthings.helm4j.repo.RepoUpdateRequest;
import dev.nthings.helm4j.types.ChartRef;
import dev.nthings.helm4j.types.ChartSource;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SdkModelCoverageTest {

  @Test
  void chartSourceBuilderAndMergeExposeNormalizedValues() {
    var base =
        ChartSource.builder()
            .version("1.2.3")
            .repositoryUrl("https://charts.example.com")
            .username("user")
            .password("pass")
            .plainHttp(true)
            .insecureSkipTlsVerification(true)
            .keyringPath("/tmp/keyring")
            .certificateFile("/tmp/cert")
            .keyFile("/tmp/key")
            .certificateAuthorityFile("/tmp/ca")
            .passCredentialsToAllHosts(true)
            .verifySignatures(true)
            .includePreReleaseVersions(true)
            .build();

    var merged =
        base.merge(
            ChartSource.builder()
                .version("2.0.0")
                .repositoryUrl("https://mirror.example.com")
                .username("other")
                .build());

    assertEquals("2.0.0", merged.version());
    assertEquals("https://mirror.example.com", merged.repositoryUrl());
    assertEquals("other", merged.username());
    assertTrue(merged.verifySignatures());
    assertTrue(merged.passCredentialsToAllHosts());
  }

  @Test
  void repoAddAndSearchBuildersCaptureAllOptions() {
    var add =
        RepoAddRequest.builder()
            .name("bitnami")
            .url("https://charts.bitnami.com/bitnami")
            .username("u")
            .password("p")
            .certificateFile("cert")
            .keyFile("key")
            .certificateAuthorityFile("ca")
            .insecureSkipTlsVerification(true)
            .passCredentialsToAllHosts(true)
            .forceUpdate(true)
            .allowDeprecatedRepositories(true)
            .timeout(Duration.ofSeconds(7))
            .build();

    assertEquals("bitnami", add.name());
    assertEquals("https://charts.bitnami.com/bitnami", add.url());
    assertTrue(add.forceUpdate());
    assertTrue(add.allowDeprecatedRepositories());
    assertEquals(Duration.ofSeconds(7), add.timeout());

    var search =
        RepoSearchRequest.builder()
            .keyword("nginx")
            .regularExpression(true)
            .includeAllVersions(true)
            .includePreReleaseVersions(true)
            .versionConstraint(">=1.0.0")
            .failIfNoResults(true)
            .maxColumnWidth(140)
            .build();

    assertEquals("nginx", search.keyword());
    assertTrue(search.regularExpression());
    assertTrue(search.includeAllVersions());
    assertTrue(search.includePreReleaseVersions());
    assertEquals(">=1.0.0", search.versionConstraint());
    assertEquals(140, search.maxColumnWidth());
  }

  @Test
  void installBuilderSupportsSourceOverrideAndStrategies() {
    var request =
        InstallRequest.builder()
            .releaseName("demo")
            .chart(ChartRef.repo("bitnami/nginx"))
            .source(
                ChartSource.builder()
                    .repositoryUrl("https://charts.bitnami.com/bitnami")
                    .version("19.0.0")
                    .build())
            .source(s -> s.username("user").password("pass").includePreReleaseVersions(true))
            .namespace("apps")
            .createNamespace(true)
            .dryRunMode(DryRunMode.SERVER)
            .waitMode(WaitMode.WATCHER)
            .waitForJobs(true)
            .timeout(Duration.ofMinutes(5))
            .description("demo install")
            .rollbackOnFailure(true)
            .skipCrds(true)
            .disableHooks(true)
            .disableOpenApiValidation(true)
            .forceReplace(true)
            .replace(true)
            .generateName(true)
            .nameTemplate("demo-{{randAlphaNum 5}}")
            .subNotes(true)
            .enableDns(true)
            .takeOwnership(true)
            .applyStrategy(ApplyStrategy.SERVER_SIDE_APPLY_FORCE_CONFLICTS)
            .values(Map.of("a", 1))
            .labels(Map.of("team", "platform"))
            .build();

    assertEquals("demo", request.releaseName());
    assertEquals("bitnami/nginx", request.chart().asReference());
    assertEquals("https://charts.bitnami.com/bitnami", request.source().repositoryUrl());
    assertEquals("user", request.source().username());
    assertTrue(request.applyStrategy().serverSideApply());
    assertTrue(request.applyStrategy().forceConflicts());
    assertEquals(DryRunMode.SERVER, request.dryRunMode());
    assertEquals(WaitMode.WATCHER, request.waitMode());
    assertTrue(request.takeOwnership());
    assertEquals(1, request.values().get("a"));
  }

  @Test
  void releaseBuildersAndRecordsCaptureAllOptions() {
    var upgrade =
        UpgradeRequest.builder()
            .releaseName("nginx")
            .chart(ChartRef.repo("bitnami/nginx"))
            .source(
                ChartSource.builder()
                    .repositoryUrl("https://charts.bitnami.com/bitnami")
                    .version("19.0.0")
                    .build())
            .source(s -> s.username("user").password("pass"))
            .namespace("apps")
            .install(true)
            .dryRunMode(DryRunMode.CLIENT)
            .waitMode(WaitMode.HOOK_ONLY)
            .waitForJobs(true)
            .timeout(Duration.ofMinutes(2))
            .description("upgrade")
            .rollbackOnFailure(true)
            .skipCrds(true)
            .disableHooks(true)
            .disableOpenApiValidation(true)
            .forceReplace(true)
            .subNotes(true)
            .enableDns(true)
            .takeOwnership(true)
            .dependencyUpdate(true)
            .cleanupOnFail(true)
            .maxHistory(7)
            .reuseValues(true)
            .resetValues(true)
            .resetThenReuseValues(true)
            .applyStrategy(ApplyStrategy.CLIENT_SIDE_APPLY)
            .values(Map.of("replicaCount", 3))
            .labels(Map.of("team", "core"))
            .build();
    assertTrue(upgrade.install());
    assertTrue(upgrade.cleanupOnFail());
    assertEquals(7, upgrade.maxHistory());
    assertEquals("user", upgrade.source().username());
    assertEquals(3, upgrade.values().get("replicaCount"));

    var rollback =
        RollbackRequest.builder()
            .releaseName("nginx")
            .namespace("apps")
            .revision(3)
            .dryRunMode(DryRunMode.SERVER)
            .timeout(Duration.ofSeconds(20))
            .waitMode(WaitMode.WATCHER)
            .waitForJobs(true)
            .disableHooks(true)
            .forceReplace(true)
            .cleanupOnFail(true)
            .maxHistory(5)
            .applyStrategy(ApplyStrategy.SERVER_SIDE_APPLY_FORCE_CONFLICTS)
            .build();
    assertEquals(3, rollback.revision());
    assertTrue(rollback.forceReplace());
    assertTrue(rollback.applyStrategy().forceConflicts());

    var uninstall =
        UninstallRequest.builder()
            .releaseName("nginx")
            .namespace("apps")
            .dryRun(true)
            .disableHooks(true)
            .keepHistory(true)
            .ignoreNotFound(true)
            .timeout(Duration.ofSeconds(30))
            .description("cleanup")
            .waitMode(WaitMode.HOOK_ONLY)
            .deletionPropagation("foreground")
            .build();
    assertTrue(uninstall.ignoreNotFound());
    assertEquals("foreground", uninstall.deletionPropagation());

    var listed =
        ReleaseListRequest.builder()
            .namespace("apps")
            .allNamespaces(true)
            .filter("nginx")
            .states(List.of("deployed", "pending"))
            .limit(50)
            .offset(3)
            .sortByDate(true)
            .sortReverse(true)
            .selector("app=nginx")
            .build();
    assertEquals(2, listed.states().size());
    assertEquals("app=nginx", listed.selector());

    var status = StatusRequest.builder().releaseName("nginx").namespace("apps").revision(9).build();
    var history = HistoryRequest.builder().releaseName("nginx").namespace("apps").max(10).build();
    var get =
        GetRequest.builder()
            .releaseName("nginx")
            .namespace("apps")
            .revision(2)
            .allValues(true)
            .build();
    var test =
        TestRequest.builder()
            .releaseName("nginx")
            .namespace("apps")
            .timeout(Duration.ofSeconds(40))
            .filter(List.of("smoke"))
            .build();
    assertEquals(9, status.revision());
    assertEquals(10, history.max());
    assertTrue(get.allValues());
    assertEquals("smoke", test.filter().getFirst());

    var hook = new HookInfo("pre-install", "Job", "templates/hook.yaml", null, 1);
    var hooks = new GetHooksResult(null);
    var release =
        new ReleaseInfo(
            "nginx",
            "apps",
            2,
            "deployed",
            "desc",
            "2026-01-01T00:00:00Z",
            "2026-01-01T00:00:00Z",
            "nginx",
            "19.0.0",
            "1.27.0",
            "notes");
    var all = new GetAllResult(release, Map.of("replicaCount", 3), "---", List.of(hook), "notes");
    var manifest = new GetManifestResult("---\napiVersion: v1");
    var notes = new GetNotesResult("release notes");
    var metadata =
        new GetMetadataResult(
            "nginx", "apps", 2, "deployed", "nginx", "19.0.0", "1.27.0", "2026-01-01");
    assertTrue(hook.events().isEmpty());
    assertTrue(hooks.hooks().isEmpty());
    assertEquals("deployed", all.release().status());
    assertTrue(manifest.manifest().contains("apiVersion"));
    assertEquals("release notes", notes.notes());
    assertEquals("nginx", metadata.name());

    var upgradeFailure = new UpgradeFailure("boom", "runOperation", "upgrade");
    var uninstallFailure = new UninstallFailure("boom", "runOperation", "uninstall");
    var rollbackFailure = new RollbackFailure("boom", "runOperation", "rollback");
    assertEquals("upgrade", upgradeFailure.operation());
    assertEquals("uninstall", uninstallFailure.operation());
    assertEquals("rollback", rollbackFailure.operation());
    assertTrue(InstallResult.class.isSealed());
  }

  @Test
  void chartAndRepoBuildersCaptureAllOptions() {
    var repoUpdate =
        RepoUpdateRequest.builder()
            .names(List.of("bitnami", "  "))
            .timeout(Duration.ofSeconds(5))
            .build();
    var repoUpdateVarargs = RepoUpdateRequest.builder().names("bitnami", "stable").build();
    var registry =
        RegistryLoginRequest.builder()
            .hostname("registry.example")
            .username("user")
            .password("secret")
            .certificateFile("cert.pem")
            .keyFile("key.pem")
            .certificateAuthorityFile("ca.pem")
            .insecure(true)
            .plainHttp(true)
            .build();
    assertEquals(List.of("bitnami"), repoUpdate.names());
    assertEquals(List.of("bitnami", "stable"), repoUpdateVarargs.names());
    assertTrue(registry.insecure());

    var hub =
        HubSearchRequest.builder()
            .keyword("nginx")
            .endpoint("https://artifacthub.io")
            .failIfNoResults(true)
            .listRepositoryUrl(true)
            .maxColumnWidth(90)
            .build();
    assertEquals("https://artifacthub.io", hub.endpoint());

    var show =
        ShowRequest.builder()
            .source(ChartSource.builder().repositoryUrl("https://charts.example.com").build())
            .source(s -> s.version("1.0.0").username("user"))
            .valuesJsonPath("{.service.type}")
            .build();
    assertEquals("{.service.type}", show.valuesJsonPath());
    assertEquals("user", show.source().username());

    var template =
        TemplateRequest.builder()
            .releaseName("nginx")
            .chart(ChartRef.repo("bitnami/nginx"))
            .source(
                ChartSource.builder()
                    .repositoryUrl("https://charts.bitnami.com/bitnami")
                    .version("19.0.0")
                    .build())
            .source(s -> s.username("template-user"))
            .namespace("apps")
            .description("render")
            .skipCrds(true)
            .disableHooks(true)
            .disableOpenApiValidation(true)
            .generateName(true)
            .nameTemplate("nginx-{{randAlphaNum 4}}")
            .subNotes(true)
            .enableDns(true)
            .includeCrds(true)
            .apiVersions(List.of("v1", "apps/v1"))
            .values(Map.of("service", Map.of("type", "ClusterIP")))
            .labels(Map.of("team", "platform"))
            .build();
    assertTrue(template.includeCrds());
    assertEquals("template-user", template.source().username());
    assertEquals(2, template.apiVersions().size());

    var pull =
        PullRequest.builder()
            .chartReference("bitnami/nginx")
            .source(
                ChartSource.builder().repositoryUrl("https://charts.bitnami.com/bitnami").build())
            .source(s -> s.version("19.0.0"))
            .untar(true)
            .untarDirectory(Path.of("tmp/untar"))
            .destinationDirectory(Path.of("tmp/dest"))
            .build();
    var pullDefaults = PullRequest.builder().chartReference("bitnami/nginx").build();
    assertTrue(pull.untar());
    assertTrue(pull.untarDirectory().isAbsolute());
    assertTrue(pull.destinationDirectory().isAbsolute());
    assertNull(pullDefaults.untarDirectory());

    var push =
        PushRequest.builder()
            .chartReference("/tmp/nginx.tgz")
            .remote("oci://registry.example/charts")
            .plainHttp(true)
            .insecureSkipTlsVerification(true)
            .certificateFile("cert.pem")
            .keyFile("key.pem")
            .certificateAuthorityFile("ca.pem")
            .build();
    assertEquals("oci://registry.example/charts", push.remote());
    assertTrue(push.insecureSkipTlsVerification());

    var packaged =
        PackageChartRequest.builder()
            .chartPath(Path.of("charts/nginx"))
            .version("19.0.0")
            .appVersion("1.27.0")
            .destination(Path.of("dist"))
            .dependencyUpdate(true)
            .sign(true)
            .key("default")
            .keyring("keyring.gpg")
            .passphraseFile("passphrase.txt")
            .plainHttp(true)
            .insecureSkipTlsVerification(true)
            .certificateFile("cert.pem")
            .keyFile("key.pem")
            .certificateAuthorityFile("ca.pem")
            .build();
    assertTrue(packaged.chartPath().isAbsolute());
    assertTrue(packaged.destination().isAbsolute());

    var dependency =
        DependencyRequest.builder()
            .chartPath(Path.of("charts/nginx"))
            .skipRefresh(true)
            .verify(true)
            .keyring("keyring.gpg")
            .plainHttp(true)
            .insecureSkipTlsVerification(true)
            .certificateFile("cert.pem")
            .keyFile("key.pem")
            .certificateAuthorityFile("ca.pem")
            .build();
    assertTrue(dependency.chartPath().isAbsolute());
    assertTrue(dependency.verify());

    var lint =
        LintRequest.builder()
            .chartPath(Path.of("charts/nginx"))
            .strict(true)
            .quiet(true)
            .withSubcharts(true)
            .values(Map.of("replicaCount", 3))
            .build();
    assertTrue(lint.chartPath().isAbsolute());
    assertTrue(lint.strict());
    assertEquals(3, lint.values().get("replicaCount"));
  }

  @Test
  void enumsAndErrorsExposeWireValues() {
    assertEquals("none", DryRunMode.NONE.wireValue());
    assertEquals("client", DryRunMode.CLIENT.wireValue());
    assertEquals("hookOnly", WaitMode.HOOK_ONLY.wireValue());
    assertFalse(ApplyStrategy.CLIENT_SIDE_APPLY.serverSideApply());
    assertEquals(LintSeverity.UNKNOWN, LintSeverity.fromWireValue(null));
    assertEquals(LintSeverity.INFO, LintSeverity.fromWireValue("info"));
    assertEquals(LintSeverity.WARNING, LintSeverity.fromWireValue("WARNING"));
    assertEquals(LintSeverity.ERROR, LintSeverity.fromWireValue("Error"));
    assertEquals(LintSeverity.UNKNOWN, LintSeverity.fromWireValue("other"));
    assertEquals("message", new LintMessage(LintSeverity.INFO, "message").message());

    var cause = new IllegalStateException("boom");
    var ex = new HelmException("msg", "stage", "operation", cause);
    assertEquals("msg", ex.getMessage());
    assertEquals("stage", ex.stage());
    assertEquals("operation", ex.operation());
    assertEquals(cause, ex.getCause());
  }
}
