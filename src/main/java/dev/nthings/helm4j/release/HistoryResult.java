package dev.nthings.helm4j.release;

import java.util.List;
import java.util.Optional;

/** Result of a release history query. */
public record HistoryResult(List<HistoryEntry> entries) {

  public HistoryResult {
    entries = entries == null ? List.of() : List.copyOf(entries);
  }

  public int size() {
    return entries.size();
  }

  public Optional<HistoryEntry> first() {
    return entries.isEmpty() ? Optional.empty() : Optional.of(entries.getFirst());
  }
}
