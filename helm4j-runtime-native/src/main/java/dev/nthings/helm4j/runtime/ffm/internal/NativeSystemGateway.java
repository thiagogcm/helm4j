package dev.nthings.helm4j.runtime.ffm.internal;

import java.util.Objects;

import dev.nthings.helm4j.spi.SystemGateway;
import dev.nthings.helm4j.system.VersionInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dev.nthings.helm4j.runtime.ffm.internal.NativeGatewaySupport.requireResponse;

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

    var response =
        requireResponse(support.convert(root, VersionPayload.class, "version"), "version", "data");
    return new VersionInfo(response.version(), response.goVersion(), response.helmVersion());
  }

  private record VersionPayload(String version, String goVersion, String helmVersion) {}
}
