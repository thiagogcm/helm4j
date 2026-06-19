package dev.nthings.helm4j.samples.support;

import java.util.stream.Collectors;

import dev.nthings.helm4j.release.Release;

/**
 * The sample suite's curated result channel.
 *
 * <p>Everything a sample wants the reader to <em>see</em> — rendered manifests, release details,
 * search hits — is printed here on {@code System.out}. Operational narration (which step is running,
 * why it failed) goes through SLF4J instead. Confining all {@code System.out} to this one class
 * makes that split explicit and easy to verify.
 */
public final class SampleOutput {

  private SampleOutput() {}

  public static void println(String text) {
    System.out.println(text);
  }

  public static void printf(String format, Object... args) {
    System.out.printf(format, args);
  }

  /** Prints a {@link Release} as a single labelled line, or a placeholder when absent. */
  public static void printRelease(String label, Release release) {
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

  /** Renders multi-line text indented under the current step, or a placeholder when empty. */
  public static String indent(String text) {
    if (text == null || text.isEmpty()) {
      return "  <empty>";
    }
    return text.lines().map(line -> "  | " + line).collect(Collectors.joining("\n"));
  }

  /** First of two strings that is non-null and non-empty, otherwise {@code ""}. */
  public static String firstNonEmpty(String a, String b) {
    if (a != null && !a.isEmpty()) {
      return a;
    }
    return b == null ? "" : b;
  }

  /** First line of {@code text}, or {@code <none>} when empty. */
  public static String firstLine(String text) {
    if (text == null || text.isEmpty()) {
      return "<none>";
    }
    int nl = text.indexOf('\n');
    return nl < 0 ? text : text.substring(0, nl);
  }

  /** Safe display name for a possibly absent release. */
  public static String releaseLabel(Release release) {
    if (release == null || release.name().isEmpty()) {
      return "<none>";
    }
    return release.name();
  }
}
