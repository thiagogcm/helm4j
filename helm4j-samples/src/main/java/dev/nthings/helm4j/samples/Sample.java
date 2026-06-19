package dev.nthings.helm4j.samples;

/**
 * A single, focused walk-through of one helm4j feature area.
 *
 * <p>Implementations narrate their steps through SLF4J and print the demonstrated results through
 * {@link dev.nthings.helm4j.samples.support.SampleOutput}. The {@link SamplesApp} dispatcher runs
 * each sample in isolation, so a failure in one sample does not abort the others.
 */
public interface Sample {

  /** Short, stable id used to select this sample from the command line (e.g. {@code charts}). */
  String id();

  /** One-line human description, shown when listing the available samples. */
  String title();

  /** What this sample needs to run; drives the {@code offline} selection group. */
  Requirement requirement();

  /** Executes the sample against the shared {@link SampleContext}. */
  void run(SampleContext context);
}
