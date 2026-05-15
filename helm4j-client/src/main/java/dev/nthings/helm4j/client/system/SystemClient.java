package dev.nthings.helm4j.client.system;

import dev.nthings.helm4j.client.internal.NamespaceClient;
import dev.nthings.helm4j.spi.SystemGateway;
import dev.nthings.helm4j.system.VersionInfo;

/** System namespace: version and (in the future) runtime capability metadata. */
public final class SystemClient extends NamespaceClient<SystemGateway> {

  public SystemClient(SystemGateway gateway) {
    super(gateway);
  }

  public VersionInfo version() {
    return gateway.version();
  }
}
