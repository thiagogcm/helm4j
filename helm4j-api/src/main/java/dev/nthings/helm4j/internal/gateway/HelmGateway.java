package dev.nthings.helm4j.internal.gateway;

/** Internal gateway composition used by the public SDK namespaces. */
public interface HelmGateway extends RepoGateway, ChartGateway, ReleaseGateway, SystemGateway {}
