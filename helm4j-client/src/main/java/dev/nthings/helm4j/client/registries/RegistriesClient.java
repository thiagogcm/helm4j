package dev.nthings.helm4j.client.registries;

import java.util.function.Consumer;

import dev.nthings.helm4j.client.internal.NamespaceClient;
import dev.nthings.helm4j.registry.RegistryLogin;
import dev.nthings.helm4j.registry.RegistryLogout;
import dev.nthings.helm4j.registry.RegistryResult;
import dev.nthings.helm4j.spi.RegistryGateway;

/** OCI registry namespace: login/logout against an OCI registry. */
public final class RegistriesClient extends NamespaceClient<RegistryGateway> {

  public RegistriesClient(RegistryGateway gateway) {
    super(gateway);
  }

  public RegistryResult login(Consumer<RegistryLogin.Builder> spec) {
    return gateway.login(configured(RegistryLogin::builder, spec).build());
  }

  public RegistryResult login(RegistryLogin request) {
    return gateway.login(request);
  }

  public RegistryResult logout(Consumer<RegistryLogout.Builder> spec) {
    return gateway.logout(configured(RegistryLogout::builder, spec).build());
  }

  public RegistryResult logout(RegistryLogout request) {
    return gateway.logout(request);
  }
}
