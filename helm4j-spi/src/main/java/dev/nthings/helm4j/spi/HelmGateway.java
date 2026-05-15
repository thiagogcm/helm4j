package dev.nthings.helm4j.spi;

/**
 * Aggregate SPI a {@link HelmGatewayProvider} hands back: one object implementing every domain
 * gateway. The {@code HelmClient} fans it out into the per-namespace clients.
 */
public interface HelmGateway extends RepoGateway, ChartGateway, ReleaseGateway, SystemGateway {}
