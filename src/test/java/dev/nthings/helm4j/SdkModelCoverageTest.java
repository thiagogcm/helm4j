package dev.nthings.helm4j;

import java.time.Duration;
import java.util.Map;

import dev.nthings.helm4j.chart.RepoSearchRequest;
import dev.nthings.helm4j.errors.HelmException;
import dev.nthings.helm4j.release.ApplyStrategy;
import dev.nthings.helm4j.release.DryRunMode;
import dev.nthings.helm4j.release.InstallRequest;
import dev.nthings.helm4j.release.WaitMode;
import dev.nthings.helm4j.repo.RepoAddRequest;
import dev.nthings.helm4j.types.ChartRef;
import dev.nthings.helm4j.types.ChartSource;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
  void enumsAndErrorsExposeWireValues() {
    assertEquals("none", DryRunMode.NONE.wireValue());
    assertEquals("client", DryRunMode.CLIENT.wireValue());
    assertEquals("hookOnly", WaitMode.HOOK_ONLY.wireValue());
    assertEquals("legacy", WaitMode.LEGACY.wireValue());
    assertFalse(ApplyStrategy.CLIENT_SIDE_APPLY.serverSideApply());

    var cause = new IllegalStateException("boom");
    var ex = new HelmException("msg", "stage", "operation", cause);
    assertEquals("msg", ex.getMessage());
    assertEquals("stage", ex.stage());
    assertEquals("operation", ex.operation());
    assertEquals(cause, ex.getCause());
  }
}
