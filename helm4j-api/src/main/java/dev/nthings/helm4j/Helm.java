package dev.nthings.helm4j;

/**
 * Standard static entry point for the Helm SDK.
 *
 * <p>Obtain a {@link HelmClient} with {@link #client()} and reach operations through its discovered
 * namespaces — {@code repo()}, {@code chart()} and {@code release()}. Each operation returns a
 * fluent, runnable request builder:
 *
 * <pre>{@code
 * try (var helm = Helm.client()) {
 *   var result = helm.release().install()
 *       .releaseName("nginx")
 *       .chart(ChartRef.repo("bitnami/nginx"))
 *       .namespace("apps")
 *       .execute();
 * }
 * }</pre>
 */
public final class Helm {

  private Helm() {}

  /** Creates a client backed by the native Helm runtime discovered on the module path. */
  public static HelmClient client() {
    return HelmClient.create();
  }

  /**
   * Returns the Helm SDK and Go runtime version info.
   *
   * <p>This opens and closes a short-lived {@link HelmClient} for the single call. Reuse a {@link
   * #client()} via try-with-resources when issuing more than one operation.
   */
  public static VersionInfo version() {
    try (var helm = Helm.client()) {
      return helm.version();
    }
  }
}
