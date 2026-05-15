package dev.nthings.helm4j.runtime.ffm.internal;

import java.util.Map;
import java.util.Objects;

import dev.nthings.helm4j.errors.HelmConfigurationException;
import dev.nthings.helm4j.registry.RegistryLogin;
import dev.nthings.helm4j.registry.RegistryLogout;
import dev.nthings.helm4j.registry.RegistryResult;
import dev.nthings.helm4j.spi.RegistryGateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dev.nthings.helm4j.runtime.ffm.internal.NativeGatewaySupport.requireResponse;
import static dev.nthings.helm4j.runtime.ffm.internal.NativeGatewaySupport.utf8;

/** OCI registry login/logout backed by the JSON bridge. */
final class NativeRegistryGateway implements RegistryGateway {

  private static final Logger log = LoggerFactory.getLogger(NativeRegistryGateway.class);

  private final NativeGatewaySupport support;

  NativeRegistryGateway(NativeGatewaySupport support) {
    this.support = Objects.requireNonNull(support, "support");
  }

  @Override
  public RegistryResult login(RegistryLogin request) {
    Objects.requireNonNull(request, "request");
    if (request.hostname() == null) {
      throw new HelmConfigurationException("Registry login requires hostname");
    }

    log.debug("Registry login: hostname={}", request.hostname());
    var root =
        support.invokeRootOrThrow(
            "registry login",
            bridge ->
                bridge.registry(
                    utf8("login"),
                    utf8(request.hostname()),
                    support.toJsonBytes(NativeOptions.registryLogin(request), "registry login")));

    var response =
        requireResponse(
            support.convert(root, RegistryPayload.class, "registry login"),
            "registry login",
            "data");
    return new RegistryResult(response.hostname(), response.status());
  }

  @Override
  public RegistryResult logout(RegistryLogout request) {
    Objects.requireNonNull(request, "request");
    if (request.hostname() == null) {
      throw new HelmConfigurationException("Registry logout requires hostname");
    }

    log.debug("Registry logout: hostname={}", request.hostname());
    var root =
        support.invokeRootOrThrow(
            "registry logout",
            bridge ->
                bridge.registry(
                    utf8("logout"),
                    utf8(request.hostname()),
                    support.toJsonBytes(Map.of(), "registry logout")));

    var response =
        requireResponse(
            support.convert(root, RegistryPayload.class, "registry logout"),
            "registry logout",
            "data");
    return new RegistryResult(response.hostname(), response.status());
  }

  private record RegistryPayload(String hostname, String status) {}
}
