package dev.nthings.helm4j.options;

/** Options accepted by {@code helm repo list}. */
public record RepoListOptions() {

  public static RepoListOptions defaults() {
    return new RepoListOptions();
  }
}
