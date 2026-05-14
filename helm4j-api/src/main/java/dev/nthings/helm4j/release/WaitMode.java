package dev.nthings.helm4j.release;

/**
 * Wait strategy supported by Helm v4 install/upgrade/rollback/uninstall actions.
 *
 * <p>Maps directly to {@code helm.sh/helm/v4/pkg/kube.WaitStrategy} on the native side. An
 * unspecified strategy lets Helm pick its default (currently {@link #WATCHER}).
 */
public enum WaitMode {
  /** Event-driven kstatus polling. Helm v4 default. */
  WATCHER("watcher"),

  /**
   * Helm-3-compatible polling loop. Use when targeting clusters where the kstatus watcher
   * misbehaves.
   */
  LEGACY("legacy"),

  /** Wait only on hook Pods/Jobs; do not block on other workload readiness. */
  HOOK_ONLY("hookOnly");

  private final String wireValue;

  WaitMode(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }
}
