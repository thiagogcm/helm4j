package dev.nthings.helm4j.release;

import java.util.List;

/** Result of running release tests. */
public record TestResult(ReleaseInfo release, List<TestHookResult> results) {

  public TestResult {
    results = results == null ? List.of() : List.copyOf(results);
  }
}
