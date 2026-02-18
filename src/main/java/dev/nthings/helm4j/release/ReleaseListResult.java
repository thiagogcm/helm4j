package dev.nthings.helm4j.release;

import java.util.List;
import java.util.Optional;

import dev.nthings.helm4j.internal.model.ModelSupport;

/** Result of listing releases. */
public record ReleaseListResult(List<ReleaseInfo> releases) {

  public ReleaseListResult {
    releases = ModelSupport.immutableListOrEmpty(releases);
  }

  public int size() {
    return releases.size();
  }

  public Optional<ReleaseInfo> first() {
    return releases.isEmpty() ? Optional.empty() : Optional.of(releases.getFirst());
  }
}
