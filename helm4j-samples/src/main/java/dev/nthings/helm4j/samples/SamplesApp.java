package dev.nthings.helm4j.samples;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import dev.nthings.helm4j.HelmClient;
import dev.nthings.helm4j.errors.HelmException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for the helm4j sample suite.
 *
 * <p>Each {@link Sample} demonstrates one SDK feature area. With no arguments every sample runs in
 * registry order; pass sample ids (e.g. {@code charts releases}) to run a subset, or {@code offline}
 * to run only the samples that need neither network nor a cluster.
 *
 * <p>Samples run in isolation: a {@link HelmException} from one is logged and the suite carries on,
 * finishing with a pass/fail summary. The release lifecycle is internally sequential, so within that
 * sample a failure still aborts the remaining steps.
 */
public final class SamplesApp {

  private static final Logger log = LoggerFactory.getLogger(SamplesApp.class);

  /** Samples in execution order. */
  private static final List<Sample> SAMPLES =
      List.of(
          new SystemSample(),
          new ChartSample(),
          new RepositorySample(),
          new ReleaseLifecycleSample());

  /** Reserved argument that selects every {@link Requirement#OFFLINE} sample. */
  private static final String OFFLINE_GROUP = "offline";

  private SamplesApp() {}

  public static void main(String[] args) {
    var selected = select(args);
    if (selected.isEmpty()) {
      log.warn("No samples matched {}", Arrays.toString(args));
      logAvailableSamples();
      return;
    }

    var failed = new ArrayList<String>();
    try (var helm = HelmClient.create()) {
      var context = new SampleContext(helm, SampleContext.locateChart());
      for (var sample : selected) {
        log.info("=== {} ({}) ===", sample.id(), sample.title());
        try {
          sample.run(context);
        } catch (HelmException e) {
          failed.add(sample.id());
          log.error("Sample {} failed: {}", sample.id(), e.getMessage(), e);
        }
      }
    }

    if (failed.isEmpty()) {
      log.info("Done: {}/{} samples succeeded", selected.size(), selected.size());
    } else {
      log.warn(
          "Done: {}/{} samples succeeded (failed: {})",
          selected.size() - failed.size(),
          selected.size(),
          String.join(", ", failed));
    }
  }

  /** Resolves command-line arguments to the ordered list of samples to run. */
  private static List<Sample> select(String[] args) {
    if (args.length == 0) {
      return SAMPLES;
    }
    var tokens = Arrays.stream(args).map(a -> a.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
    if (tokens.contains(OFFLINE_GROUP)) {
      return SAMPLES.stream().filter(s -> s.requirement() == Requirement.OFFLINE).toList();
    }
    return SAMPLES.stream().filter(s -> tokens.contains(s.id())).toList();
  }

  private static void logAvailableSamples() {
    var available =
        SAMPLES.stream()
            .map(s -> "  " + s.id() + " - " + s.title())
            .collect(Collectors.joining(System.lineSeparator()));
    log.info(
        "Usage: [<id>...] | {} | (no args = run all){}Available samples:{}{}",
        OFFLINE_GROUP,
        System.lineSeparator(),
        System.lineSeparator(),
        available);
  }
}
