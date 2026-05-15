package dev.nthings.helm4j.samples;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.nthings.helm4j.HelmClient;
import dev.nthings.helm4j.chart.ChartRef;
import dev.nthings.helm4j.chart.ShowMode;
import dev.nthings.helm4j.errors.HelmCommandException;
import dev.nthings.helm4j.release.Release;

/**
 * Comprehensive walk-through of the helm4j public API against a real cluster.
 *
 * <p>Each section exercises a different namespace of the SDK; the run output doubles as a feature
 * checklist. Mutations throw {@link HelmCommandException} on failure, so the sample reads as
 * straight-line code instead of pattern-matching over sealed result wrappers.
 */
public final class HelloHelm {

  private static final String NAMESPACE = "helm4j-samples";
  private static final String RELEASE = "hello-world";
  private static final String REPO_NAME = "ingress-nginx";
  private static final String REPO_URL = "https://kubernetes.github.io/ingress-nginx";

  private HelloHelm() {}

  public static void main(String[] args) throws IOException {
    var chartPath = locateChart();

    try (var helm = HelmClient.create()) {
      printVersion(helm);
      runChartOps(helm, chartPath);
      runRepoOps(helm);
      runReleaseOps(helm, chartPath);
    }

    System.out.println("\n[done] sample completed");
  }

  private static void printVersion(HelmClient helm) {
    section("system.version");
    var version = helm.system().version();
    System.out.printf(
        "  helm4j=%s helm=%s go=%s%n",
        version.version(), version.helmVersion(), version.goVersion());
  }

  private static void runChartOps(HelmClient helm, Path chartPath) throws IOException {
    section("charts.show");
    var local = ChartRef.local(chartPath);
    var chartSection = helm.charts().show(ShowMode.CHART, local, b -> {});
    System.out.println(indent(firstNonEmpty(chartSection.metadataYaml(), chartSection.rawOutput())));

    var valuesSection = helm.charts().show(ShowMode.VALUES, local, b -> {});
    System.out.println(indent(firstNonEmpty(valuesSection.valuesYaml(), valuesSection.rawOutput())));

    var readmeSection = helm.charts().show(ShowMode.README, local, b -> {});
    System.out.println(indent(firstNonEmpty(readmeSection.readmeText(), readmeSection.rawOutput())));

    section("charts.lint");
    var lint = helm.charts().lint(b -> b.chartPath(chartPath).strict(true));
    System.out.printf(
        "  passed=%s total=%d tested=%d failed=%d messages=%d%n",
        lint.passed(),
        lint.totalCharts(),
        lint.chartsTested(),
        lint.chartsFailed(),
        lint.messages().size());

    section("charts.template");
    var template =
        helm.charts()
            .template(
                b ->
                    b.releaseName("render-only")
                        .chart(local)
                        .namespace(NAMESPACE)
                        .values(Map.of("message", "rendered offline")));
    System.out.printf("  manifest bytes=%d%n", template.manifest().length());

    section("charts.packageChart");
    var pkgDir = Files.createTempDirectory("helm4j-samples-pkg-");
    deleteOnExit(pkgDir);
    var pkg = helm.charts().packageChart(b -> b.chartPath(chartPath).destination(pkgDir));
    System.out.printf("  produced %s%n", pkg.path());

    section("charts.dependency");
    var dependency = helm.charts().dependency(b -> b.chartPath(chartPath).skipRefresh(true));
    System.out.println(indent(firstNonEmpty(dependency.output(), "<no dependency output>")));
  }

  private static void runRepoOps(HelmClient helm) {
    section("repositories.add");
    var added = helm.repositories().add(b -> b.name(REPO_NAME).url(REPO_URL).forceUpdate(true));
    System.out.printf("  added %s -> %s%n", added.name(), added.url());

    section("repositories.list");
    helm.repositories()
        .list()
        .forEach(repo -> System.out.printf("  - %s %s%n", repo.name(), repo.url()));

    section("repositories.update");
    helm.repositories()
        .update(b -> b.names(REPO_NAME))
        .forEach(entry -> System.out.printf("  - %s: %s%n", entry.name(), entry.status()));

    section("charts.searchRepository");
    var hits = helm.charts().searchRepository(b -> b.keyword(REPO_NAME).maxColumnWidth(80));
    System.out.printf("  %d hits, first 3:%n", hits.size());
    hits.stream()
        .limit(3)
        .forEach(
            chart ->
                System.out.printf(
                    "    - %s %s (app %s)%n",
                    chart.name(), chart.version(), chart.appVersion()));

    section("repositories.remove");
    helm.repositories()
        .remove(b -> b.names(REPO_NAME))
        .forEach(removed -> System.out.printf("  removed %s%n", removed));
  }

  private static void runReleaseOps(HelmClient helm, Path chartPath) {
    var chart = ChartRef.local(chartPath);

    section("releases.uninstall (cleanup)");
    var cleanup =
        helm.releases()
            .uninstall(b -> b.releaseName(RELEASE).namespace(NAMESPACE).ignoreNotFound(true));
    System.out.printf("  pre-existing cleared: %s%n", releaseLabel(cleanup.release()));

    section("releases.install");
    var installed =
        helm.releases()
            .install(
                b ->
                    b.releaseName(RELEASE)
                        .chart(chart)
                        .namespace(NAMESPACE)
                        .createNamespace(true)
                        .description("hello-world install")
                        .labels(Map.of("sample", "hello-world"))
                        .values(Map.of("message", "hello, helm4j!")));
    printRelease("installed", installed);

    section("releases.list");
    var releases = helm.releases().list(b -> b.namespace(NAMESPACE));
    releases.forEach(r -> System.out.printf("  - %s rev %d (%s)%n", r.name(), r.revision(), r.status()));

    section("releases.status");
    var status = helm.releases().status(b -> b.releaseName(RELEASE).namespace(NAMESPACE));
    printRelease("status", status);

    section("releases.getAll");
    var getAll =
        helm.releases()
            .getAll(b -> b.releaseName(RELEASE).namespace(NAMESPACE).allValues(true));
    System.out.printf(
        "  values=%d manifestBytes=%d hooks=%d notes=%s%n",
        getAll.values().size(),
        getAll.manifest().length(),
        getAll.hooks().size(),
        firstLine(getAll.notes()));

    section("releases.getValues");
    var values = helm.releases().getValues(b -> b.releaseName(RELEASE).namespace(NAMESPACE));
    System.out.printf("  %s%n", values.values());

    section("releases.getManifest");
    var manifest = helm.releases().getManifest(b -> b.releaseName(RELEASE).namespace(NAMESPACE));
    System.out.printf("  bytes=%d%n", manifest.manifest().length());

    section("releases.getHooks");
    var hooks = helm.releases().getHooks(b -> b.releaseName(RELEASE).namespace(NAMESPACE));
    hooks
        .hooks()
        .forEach(
            h ->
                System.out.printf(
                    "  - %s %s events=%s weight=%d%n", h.kind(), h.name(), h.events(), h.weight()));

    section("releases.getNotes");
    var notes = helm.releases().getNotes(b -> b.releaseName(RELEASE).namespace(NAMESPACE));
    System.out.println(indent(notes.notes()));

    section("releases.getMetadata");
    var metadata = helm.releases().getMetadata(b -> b.releaseName(RELEASE).namespace(NAMESPACE));
    System.out.printf(
        "  %s/%s rev %d chart=%s-%s app=%s status=%s%n",
        metadata.namespace(),
        metadata.name(),
        metadata.revision(),
        metadata.chart(),
        metadata.chartVersion(),
        metadata.appVersion(),
        metadata.status());

    section("releases.upgrade");
    var upgraded =
        helm.releases()
            .upgrade(
                b ->
                    b.releaseName(RELEASE)
                        .chart(chart)
                        .namespace(NAMESPACE)
                        .resetValues(true)
                        .values(Map.of("message", "hello again, helm4j!"))
                        .description("flip the message"));
    printRelease("upgraded", upgraded);

    section("releases.history");
    var history = helm.releases().history(b -> b.releaseName(RELEASE).namespace(NAMESPACE).max(10));
    history.forEach(
        h ->
            System.out.printf(
                "  rev %d %s %s%n",
                h.revision(), h.status(), firstNonEmpty(h.description(), "")));

    section("releases.rollback");
    var rolledBack =
        helm.releases()
            .rollback(b -> b.releaseName(RELEASE).namespace(NAMESPACE).revision(1));
    System.out.printf("  rolled back %s to rev %d%n", rolledBack.releaseName(), rolledBack.revision());

    section("releases.test");
    var tests = helm.releases().test(b -> b.releaseName(RELEASE).namespace(NAMESPACE));
    tests
        .results()
        .forEach(t -> System.out.printf("  - %s %s%n", t.name(), t.status()));
    if (tests.results().isEmpty()) {
      System.out.println("  (no test hooks reported)");
    }

    section("releases.uninstall");
    var uninstalled = helm.releases().uninstall(b -> b.releaseName(RELEASE).namespace(NAMESPACE));
    System.out.printf("  uninstalled %s%n", releaseLabel(uninstalled.release()));
  }

  private static void section(String title) {
    System.out.println();
    System.out.println("== " + title + " ==");
  }

  private static void printRelease(String label, Release release) {
    if (release == null) {
      System.out.printf("  %s: <none>%n", label);
      return;
    }
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

  private static String releaseLabel(Release release) {
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
