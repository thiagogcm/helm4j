package dev.nthings.helm4j.samples;

import java.util.Map;

import dev.nthings.helm4j.chart.ChartRef;
import dev.nthings.helm4j.chart.ShowMode;
import dev.nthings.helm4j.samples.support.SampleOutput;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Offline chart operations: inspect metadata / values / readme, lint, render templates, package, and
 * resolve dependencies. None of these need a cluster, so this sample is part of the {@code offline}
 * group.
 */
public final class ChartSample implements Sample {

  private static final Logger log = LoggerFactory.getLogger(ChartSample.class);

  @Override
  public String id() {
    return "charts";
  }

  @Override
  public String title() {
    return "charts: show, lint, template, package, dependency (offline)";
  }

  @Override
  public Requirement requirement() {
    return Requirement.OFFLINE;
  }

  @Override
  public void run(SampleContext context) {
    var charts = context.helm().charts();
    var chartPath = context.chartPath();
    var local = ChartRef.local(chartPath);

    log.info("Showing chart metadata, values and readme for {}", chartPath);
    var chartSection = charts.show(ShowMode.CHART, local, b -> {});
    SampleOutput.println(
        SampleOutput.indent(
            SampleOutput.firstNonEmpty(chartSection.metadataYaml(), chartSection.rawOutput())));
    var valuesSection = charts.show(ShowMode.VALUES, local, b -> {});
    SampleOutput.println(
        SampleOutput.indent(
            SampleOutput.firstNonEmpty(valuesSection.valuesYaml(), valuesSection.rawOutput())));
    var readmeSection = charts.show(ShowMode.README, local, b -> {});
    SampleOutput.println(
        SampleOutput.indent(
            SampleOutput.firstNonEmpty(readmeSection.readmeText(), readmeSection.rawOutput())));

    log.info("Linting chart (strict)");
    var lint = charts.lint(b -> b.chartPath(chartPath).strict(true));
    SampleOutput.printf(
        "  passed=%s total=%d tested=%d failed=%d messages=%d%n",
        lint.passed(),
        lint.totalCharts(),
        lint.chartsTested(),
        lint.chartsFailed(),
        lint.messages().size());

    log.info("Rendering templates offline");
    var template =
        charts
            .template(
                b ->
                    b.releaseName("render-only")
                        .chart(local)
                        .namespace(context.namespace())
                        .values(Map.of("message", "rendered offline")));
    SampleOutput.printf("  manifest bytes=%d%n", template.manifest().length());

    log.info("Packaging chart");
    var pkgDir = context.newTempDirectory("helm4j-samples-pkg-");
    var pkg = charts.packageChart(b -> b.chartPath(chartPath).destination(pkgDir));
    SampleOutput.printf("  produced %s%n", pkg.path());

    log.info("Resolving chart dependencies");
    var dependency = charts.dependency(b -> b.chartPath(chartPath).skipRefresh(true));
    SampleOutput.println(
        SampleOutput.indent(
            SampleOutput.firstNonEmpty(dependency.output(), "<no dependency output>")));
  }
}
