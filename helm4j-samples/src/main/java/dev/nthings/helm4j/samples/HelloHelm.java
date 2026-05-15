package dev.nthings.helm4j.samples;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.nthings.helm4j.chart.ChartRef;
import dev.nthings.helm4j.chart.ShowMode;
import dev.nthings.helm4j.client.Helm;
import dev.nthings.helm4j.client.HelmClient;
import dev.nthings.helm4j.release.ReleaseFailure;
import dev.nthings.helm4j.release.ReleaseInfo;
import dev.nthings.helm4j.release.ReleasePending;
import dev.nthings.helm4j.release.ReleaseSuccess;
import dev.nthings.helm4j.release.RollbackFailure;
import dev.nthings.helm4j.release.RollbackSuccess;
import dev.nthings.helm4j.release.UninstallFailure;
import dev.nthings.helm4j.release.UninstallSuccess;
import dev.nthings.helm4j.repo.RepoAddFailure;
import dev.nthings.helm4j.repo.RepoAddSuccess;

/**
 * Comprehensive walk-through of the helm4j public API against a real cluster. Each section
 * exercises a different namespace of the SDK and prints a short summary so the run output doubles
 * as a feature checklist.
 */
public final class HelloHelm {

  private static final String NAMESPACE = "helm4j-samples";
  private static final String RELEASE = "hello-world";
  private static final String REPO_NAME = "ingress-nginx";
  private static final String REPO_URL = "https://kubernetes.github.io/ingress-nginx";

  private HelloHelm() {}

  public static void main(String[] args) throws IOException {
    var chartPath = locateChart();

    try (var helm = Helm.client()) {
      printVersion(helm);
      runChartOps(helm, chartPath);
      runRepoOps(helm);
      runReleaseOps(helm, chartPath);
    }

    System.out.println("\n[done] sample completed");
  }

  private static void printVersion(HelmClient helm) {
    section("system.version");
    var version = helm.version();
    System.out.printf(
        "  helm4j=%s helm=%s go=%s%n",
        version.version(), version.helmVersion(), version.goVersion());
  }

  private static void runChartOps(HelmClient helm, Path chartPath) throws IOException {
    section("chart.show");
    var local = ChartRef.local(chartPath);
    var chartSection = helm.chart().show(ShowMode.CHART, local, b -> {});
    System.out.println(indent(firstNonEmpty(chartSection.metadataYaml(), chartSection.rawOutput())));

    var valuesSection = helm.chart().show(ShowMode.VALUES, local, b -> {});
    System.out.println(indent(firstNonEmpty(valuesSection.valuesYaml(), valuesSection.rawOutput())));

    var readmeSection = helm.chart().show(ShowMode.README, local, b -> {});
    System.out.println(indent(firstNonEmpty(readmeSection.readmeText(), readmeSection.rawOutput())));

    section("chart.lint");
    var lint = helm.chart().lint(b -> b.chartPath(chartPath).strict(true));
    System.out.printf(
        "  passed=%s total=%d tested=%d failed=%d messages=%d%n",
        lint.passed(),
        lint.totalCharts(),
        lint.chartsTested(),
        lint.chartsFailed(),
        lint.messages().size());

    section("chart.template");
    var template =
        helm.chart()
            .template(
                b ->
                    b.releaseName("render-only")
                        .chart(local)
                        .namespace(NAMESPACE)
                        .values(Map.of("message", "rendered offline")));
    System.out.printf("  manifest bytes=%d%n", template.manifest().length());

    section("chart.packageChart");
    var pkgDir = Files.createTempDirectory("helm4j-samples-pkg-");
    deleteOnExit(pkgDir);
    var pkg = helm.chart().packageChart(b -> b.chartPath(chartPath).destination(pkgDir));
    System.out.printf("  produced %s%n", pkg.path());

    section("chart.dependency");
    var dependency = helm.chart().dependency(b -> b.chartPath(chartPath).skipRefresh(true));
    System.out.println(indent(firstNonEmpty(dependency.output(), "<no dependency output>")));
  }

  private static void runRepoOps(HelmClient helm) {
    section("repo.add");
    var addResult =
        helm.repo()
            .add(b -> b.name(REPO_NAME).url(REPO_URL).forceUpdate(true));
    switch (addResult) {
      case RepoAddSuccess success ->
          System.out.printf("  added %s -> %s%n", success.name(), success.url());
      case RepoAddFailure failure ->
          throw new IllegalStateException("repo add failed: " + failure.failure());
    }

    section("repo.list");
    helm.repo()
        .list()
        .forEach(repo -> System.out.printf("  - %s %s%n", repo.name(), repo.url()));

    section("repo.update");
    helm.repo()
        .update(b -> b.names(REPO_NAME))
        .forEach(entry -> System.out.printf("  - %s: %s%n", entry.name(), entry.status()));

    section("chart.searchRepo");
    var hits =
        helm.chart()
            .searchRepo(b -> b.keyword(REPO_NAME).maxColumnWidth(80));
    System.out.printf("  %d hits, first 3:%n", hits.size());
    hits.stream()
        .limit(3)
        .forEach(
            chart ->
                System.out.printf(
                    "    - %s %s (app %s)%n",
                    chart.name(), chart.version(), chart.appVersion()));

    section("repo.remove");
    helm.repo()
        .remove(b -> b.names(REPO_NAME))
        .forEach(removed -> System.out.printf("  removed %s%n", removed));
  }

  private static void runReleaseOps(HelmClient helm, Path chartPath) {
    var chart = ChartRef.local(chartPath);

    section("release.uninstall (cleanup)");
    var cleanup =
        helm.release()
            .uninstall(b -> b.releaseName(RELEASE).namespace(NAMESPACE).ignoreNotFound(true));
    switch (cleanup) {
      case UninstallSuccess success ->
          System.out.printf("  pre-existing cleared: %s%n", releaseLabel(success.release()));
      case UninstallFailure failure ->
          throw new IllegalStateException("cleanup uninstall failed: " + failure.failure());
    }

    section("release.install");
    var installed =
        helm.release()
            .install(
                b ->
                    b.releaseName(RELEASE)
                        .chart(chart)
                        .namespace(NAMESPACE)
                        .createNamespace(true)
                        .description("hello-world install")
                        .labels(Map.of("sample", "hello-world"))
                        .values(Map.of("message", "hello, helm4j!")));
    switch (installed) {
      case ReleaseSuccess success -> printRelease("installed", success.release());
      case ReleasePending pending -> printRelease("pending", pending.release());
      case ReleaseFailure failure ->
          throw new IllegalStateException("install failed: " + failure.failure());
    }

    section("release.list");
    var releases = helm.release().list(b -> b.namespace(NAMESPACE));
    releases.forEach(r -> System.out.printf("  - %s rev %d (%s)%n", r.name(), r.revision(), r.status()));

    section("release.status");
    var status = helm.release().status(b -> b.releaseName(RELEASE).namespace(NAMESPACE));
    printRelease("status", status.release());

    section("release.getAll");
    var getAll =
        helm.release().getAll(b -> b.releaseName(RELEASE).namespace(NAMESPACE).allValues(true));
    System.out.printf(
        "  values=%d manifestBytes=%d hooks=%d notes=%s%n",
        getAll.values().size(),
        getAll.manifest().length(),
        getAll.hooks().size(),
        firstLine(getAll.notes()));

    section("release.getValues");
    var values = helm.release().getValues(b -> b.releaseName(RELEASE).namespace(NAMESPACE));
    System.out.printf("  %s%n", values.values());

    section("release.getManifest");
    var manifest = helm.release().getManifest(b -> b.releaseName(RELEASE).namespace(NAMESPACE));
    System.out.printf("  bytes=%d%n", manifest.manifest().length());

    section("release.getHooks");
    var hooks = helm.release().getHooks(b -> b.releaseName(RELEASE).namespace(NAMESPACE));
    hooks
        .hooks()
        .forEach(
            h ->
                System.out.printf("  - %s %s events=%s weight=%d%n", h.kind(), h.name(), h.events(), h.weight()));

    section("release.getNotes");
    var notes = helm.release().getNotes(b -> b.releaseName(RELEASE).namespace(NAMESPACE));
    System.out.println(indent(notes.notes()));

    section("release.getMetadata");
    var metadata = helm.release().getMetadata(b -> b.releaseName(RELEASE).namespace(NAMESPACE));
    System.out.printf(
        "  %s/%s rev %d chart=%s-%s app=%s status=%s%n",
        metadata.namespace(),
        metadata.name(),
        metadata.revision(),
        metadata.chart(),
        metadata.chartVersion(),
        metadata.appVersion(),
        metadata.status());

    section("release.upgrade");
    var upgraded =
        helm.release()
            .upgrade(
                b ->
                    b.releaseName(RELEASE)
                        .chart(chart)
                        .namespace(NAMESPACE)
                        .resetValues(true)
                        .values(Map.of("message", "hello again, helm4j!"))
                        .description("flip the message"));
    switch (upgraded) {
      case ReleaseSuccess success -> printRelease("upgraded", success.release());
      case ReleasePending pending -> printRelease("upgrade pending", pending.release());
      case ReleaseFailure failure ->
          throw new IllegalStateException("upgrade failed: " + failure.failure());
    }

    section("release.history");
    var history = helm.release().history(b -> b.releaseName(RELEASE).namespace(NAMESPACE).max(10));
    history.forEach(
        h ->
            System.out.printf(
                "  rev %d %s %s%n",
                h.revision(), h.status(), firstNonEmpty(h.description(), "")));

    section("release.rollback");
    var rolledBack =
        helm.release()
            .rollback(b -> b.releaseName(RELEASE).namespace(NAMESPACE).revision(1));
    switch (rolledBack) {
      case RollbackSuccess success ->
          System.out.printf("  rolled back %s to rev %d%n", success.releaseName(), success.revision());
      case RollbackFailure failure ->
          throw new IllegalStateException("rollback failed: " + failure.failure());
    }

    section("release.test");
    var tests = helm.release().test(b -> b.releaseName(RELEASE).namespace(NAMESPACE));
    tests
        .results()
        .forEach(t -> System.out.printf("  - %s %s%n", t.name(), t.status()));
    if (tests.results().isEmpty()) {
      System.out.println("  (no test hooks reported)");
    }

    section("release.uninstall");
    var uninstalled = helm.release().uninstall(b -> b.releaseName(RELEASE).namespace(NAMESPACE));
    switch (uninstalled) {
      case UninstallSuccess success ->
          System.out.printf("  uninstalled %s%n", releaseLabel(success.release()));
      case UninstallFailure failure ->
          throw new IllegalStateException("uninstall failed: " + failure.failure());
    }
  }

  private static void section(String title) {
    System.out.println();
    System.out.println("== " + title + " ==");
  }

  private static void printRelease(String label, ReleaseInfo release) {
    System.out.printf(
        "  %s: %s/%s rev %d %s (%s %s)%n",
        label,
        release.namespace(),
        release.name(),
        release.revision(),
        release.status(),
        release.chartName(),
        release.chartVersion());
  }

  private static String indent(String text) {
    if (text == null || text.isEmpty()) {
      return "  <empty>";
    }
    return text.lines().map(line -> "  | " + line).collect(Collectors.joining("\n"));
  }

  private static String firstNonEmpty(String a, String b) {
    if (a != null && !a.isEmpty()) {
      return a;
    }
    return b == null ? "" : b;
  }

  private static String firstLine(String text) {
    if (text == null || text.isEmpty()) {
      return "<none>";
    }
    int nl = text.indexOf('\n');
    return nl < 0 ? text : text.substring(0, nl);
  }

  private static String releaseLabel(ReleaseInfo release) {
    if (release == null || release.name().isEmpty()) {
      return "<none>";
    }
    return release.name();
  }

  private static void deleteOnExit(Path root) {
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  try (Stream<Path> walk = Files.walk(root)) {
                    walk.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
                  } catch (IOException ignored) {
                  }
                },
                "helm4j-samples-cleanup"));
  }

  private static Path locateChart() {
    var configured = System.getProperty("helm4j.samples.chart");
    if (configured != null && !configured.isBlank()) {
      return Path.of(configured);
    }
    return Path.of("helm4j-samples/src/main/resources/charts/hello-world");
  }
}
