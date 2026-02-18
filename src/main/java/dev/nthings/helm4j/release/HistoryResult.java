package dev.nthings.helm4j.release;

import java.util.List;
import java.util.Optional;

import dev.nthings.helm4j.internal.model.ModelSupport;

/** Result of a release history query. */
public record HistoryResult(List<HistoryEntry> entries) {

  public HistoryResult {
    entries = ModelSupport.immutableListOrEmpty(entries);
  }

  public int size() {
    return entries.size();
  }

  public Optional<HistoryEntry> first() {
    return entries.isEmpty() ? Optional.empty() : Optional.of(entries.getFirst());
  }
}
