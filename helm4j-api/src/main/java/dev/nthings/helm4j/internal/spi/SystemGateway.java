package dev.nthings.helm4j.internal.spi;

import dev.nthings.helm4j.VersionInfo;

/** Internal system-level operations. */
public interface SystemGateway {

  VersionInfo version();
}
