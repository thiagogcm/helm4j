package dev.nthings.helm4j.samples;

import dev.nthings.helm4j.samples.support.SampleOutput;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Reports the helm4j / Helm / Go versions exposed by {@code helm.system()}. Runs offline. */
public final class SystemSample implements Sample {

  private static final Logger log = LoggerFactory.getLogger(SystemSample.class);

  @Override
  public String id() {
    return "system";
  }

  @Override
  public String title() {
    return "system: runtime version information";
  }

  @Override
  public Requirement requirement() {
    return Requirement.OFFLINE;
  }

  @Override
  public void run(SampleContext context) {
    log.info("Querying runtime version");
    var version = context.helm().system().version();
    SampleOutput.printf(
        "  helm4j=%s helm=%s go=%s%n",
        version.version(), version.helmVersion(), version.goVersion());
  }
}
