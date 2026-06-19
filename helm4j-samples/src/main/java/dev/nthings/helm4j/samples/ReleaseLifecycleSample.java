package dev.nthings.helm4j.samples;

import java.util.Map;

import dev.nthings.helm4j.chart.ChartRef;
import dev.nthings.helm4j.samples.support.SampleOutput;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Full release lifecycle against a live cluster: clear any prior release, install, inspect it every
 * way the SDK allows, upgrade, review history, roll back, run test hooks, and uninstall.
 *
 * <p>The steps run straight-line because each depends on the previous one (you cannot upgrade a
 * release that failed to install). A failure aborts this sample, but {@link SamplesApp} keeps the
 * rest of the suite running.
 */
public final class ReleaseLifecycleSample implements Sample {

  private static final Logger log = LoggerFactory.getLogger(ReleaseLifecycleSample.class);

  private static final String RELEASE = "hello-world";

  @Override
  public String id() {
    return "releases";
  }

  @Override
  public String title() {
    return "releases: install -> inspect -> upgrade -> rollback -> test -> uninstall (cluster)";
  }

  @Override
  public Requirement requirement() {
    return Requirement.CLUSTER;
  }

  @Override
  public void run(SampleContext context) {
    var releases = context.helm().releases();
    var namespace = context.namespace();
    var chart = ChartRef.local(context.chartPath());

    log.info("Clearing any pre-existing release {} in {}", RELEASE, namespace);
    var cleanup =
        releases.uninstall(b -> b.releaseName(RELEASE).namespace(namespace).ignoreNotFound(true));
    SampleOutput.printf(
        "  pre-existing cleared: %s%n", SampleOutput.releaseLabel(cleanup.release()));

    log.info("Installing release {} in {}", RELEASE, namespace);
    var installed =
        releases.install(
            b ->
                b.releaseName(RELEASE)
                    .chart(chart)
                    .namespace(namespace)
                    .createNamespace(true)
                    .description("hello-world install")
                    .labels(Map.of("sample", "hello-world"))
                    .values(Map.of("message", "hello, helm4j!")));
    SampleOutput.printRelease("installed", installed);

    log.info("Listing releases in {}", namespace);
    releases
        .list(b -> b.namespace(namespace))
        .forEach(
            r -> SampleOutput.printf("  - %s rev %d (%s)%n", r.name(), r.revision(), r.status()));

    log.info("Fetching release status");
    var status = releases.status(b -> b.releaseName(RELEASE).namespace(namespace));
    SampleOutput.printRelease("status", status);

    log.info("Inspecting release (getAll)");
    var getAll =
        releases.getAll(b -> b.releaseName(RELEASE).namespace(namespace).allValues(true));
    SampleOutput.printf(
        "  values=%d manifestBytes=%d hooks=%d notes=%s%n",
        getAll.values().size(),
        getAll.manifest().length(),
        getAll.hooks().size(),
        SampleOutput.firstLine(getAll.notes()));

    log.info("Inspecting release values");
    var values = releases.getValues(b -> b.releaseName(RELEASE).namespace(namespace));
    SampleOutput.printf("  %s%n", values.values());

    log.info("Inspecting release manifest");
    var manifest = releases.getManifest(b -> b.releaseName(RELEASE).namespace(namespace));
    SampleOutput.printf("  bytes=%d%n", manifest.manifest().length());

    log.info("Inspecting release hooks");
    releases
        .getHooks(b -> b.releaseName(RELEASE).namespace(namespace))
        .hooks()
        .forEach(
            h ->
                SampleOutput.printf(
                    "  - %s %s events=%s weight=%d%n", h.kind(), h.name(), h.events(), h.weight()));

    log.info("Inspecting release notes");
    var notes = releases.getNotes(b -> b.releaseName(RELEASE).namespace(namespace));
    SampleOutput.println(SampleOutput.indent(notes.notes()));

    log.info("Inspecting release metadata");
    var metadata = releases.getMetadata(b -> b.releaseName(RELEASE).namespace(namespace));
    SampleOutput.printf(
        "  %s/%s rev %d chart=%s-%s app=%s status=%s%n",
        metadata.namespace(),
        metadata.name(),
        metadata.revision(),
        metadata.chart(),
        metadata.chartVersion(),
        metadata.appVersion(),
        metadata.status());

    log.info("Upgrading release {}", RELEASE);
    var upgraded =
        releases.upgrade(
            b ->
                b.releaseName(RELEASE)
                    .chart(chart)
                    .namespace(namespace)
                    .resetValues(true)
                    .values(Map.of("message", "hello again, helm4j!"))
                    .description("flip the message"));
    SampleOutput.printRelease("upgraded", upgraded);

    log.info("Reviewing release history");
    releases
        .history(b -> b.releaseName(RELEASE).namespace(namespace).max(10))
        .forEach(
            h ->
                SampleOutput.printf(
                    "  rev %d %s %s%n",
                    h.revision(), h.status(), SampleOutput.firstNonEmpty(h.description(), "")));

    log.info("Rolling back release {} to revision 1", RELEASE);
    var rolledBack =
        releases.rollback(b -> b.releaseName(RELEASE).namespace(namespace).revision(1));
    SampleOutput.printf(
        "  rolled back %s to rev %d%n", rolledBack.releaseName(), rolledBack.revision());

    log.info("Running release test hooks");
    var tests = releases.test(b -> b.releaseName(RELEASE).namespace(namespace));
    tests.results().forEach(t -> SampleOutput.printf("  - %s %s%n", t.name(), t.status()));
    if (tests.results().isEmpty()) {
      log.warn("No test hooks reported for {}", RELEASE);
    }

    log.info("Uninstalling release {}", RELEASE);
    var uninstalled = releases.uninstall(b -> b.releaseName(RELEASE).namespace(namespace));
    SampleOutput.printf("  uninstalled %s%n", SampleOutput.releaseLabel(uninstalled.release()));
  }
}
