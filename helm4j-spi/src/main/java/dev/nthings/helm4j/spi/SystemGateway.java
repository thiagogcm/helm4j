package dev.nthings.helm4j.spi;

import dev.nthings.helm4j.VersionInfo;

/** SPI for system-level operations, backing {@code HelmClient.version()}. */
public interface SystemGateway {

  VersionInfo version();
}
