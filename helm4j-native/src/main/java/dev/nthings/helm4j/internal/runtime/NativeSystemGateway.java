package dev.nthings.helm4j.internal.runtime;

import java.util.Objects;

import dev.nthings.helm4j.VersionInfo;
import dev.nthings.helm4j.errors.HelmException;
import dev.nthings.helm4j.internal.gateway.SystemGateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** System-level operations backed by the JSON bridge. */
final class NativeSystemGateway implements SystemGateway {

  private static final Logger log = LoggerFactory.getLogger(NativeSystemGateway.class);

  private final NativeGatewaySupport support;

  NativeSystemGateway(NativeGatewaySupport support) {
    this.support = Objects.requireNonNull(support, "support");
  }

  @Override
  public VersionInfo version() {
    log.debug("Getting version info");
    var root = support.invokeRootOrThrow("version", HelmBridge::version);

    var response = support.convert(root, VersionPayload.class, "version");
    if (response == null) {
      throw new HelmException("Native version response missing data", "decodeResponse", "version");
    }
    return new VersionInfo(response.version(), response.goVersion(), response.helmVersion());
  }

  private record VersionPayload(String version, String goVersion, String helmVersion) {}
}
