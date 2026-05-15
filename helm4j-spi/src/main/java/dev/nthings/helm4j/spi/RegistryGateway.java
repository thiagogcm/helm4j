package dev.nthings.helm4j.spi;

import dev.nthings.helm4j.registry.RegistryLogin;
import dev.nthings.helm4j.registry.RegistryLogout;
import dev.nthings.helm4j.registry.RegistryResult;

/** SPI for OCI registry login/logout. Distinct from {@link RepositoryGateway}. */
public interface RegistryGateway {

  RegistryResult login(RegistryLogin request);

  RegistryResult logout(RegistryLogout request);
}
