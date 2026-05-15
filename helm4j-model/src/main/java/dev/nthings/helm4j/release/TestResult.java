package dev.nthings.helm4j.release;

import java.util.List;

import dev.nthings.helm4j.internal.model.ModelSupport;

/** Result of running release tests. */
public record TestResult(Release release, List<TestHookResult> results) {

  public TestResult {
    results = ModelSupport.immutableListOrEmpty(results);
  }
}
