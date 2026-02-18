package dev.nthings.helm4j.repo;

import java.util.Objects;
import java.util.function.Consumer;

import dev.nthings.helm4j.internal.api.ClientSupport;
import dev.nthings.helm4j.internal.sdk.HelmGateway;

/** Repository and registry namespace for Helm SDK operations. */
public final class RepoClient {

  private final HelmGateway gateway;

  public RepoClient(HelmGateway gateway) {
    this.gateway = Objects.requireNonNull(gateway, "gateway");
  }

  public RepoAddResult add(Consumer<RepoAddRequest.Builder> spec) {
    return ClientSupport.buildAndCall(
        RepoAddRequest::builder, spec, RepoAddRequest.Builder::build, this::add);
  }

  public RepoAddResult add(RepoAddRequest request) {
    Objects.requireNonNull(request, "request");
    return gateway.repoAdd(request);
  }

  public RepoUpdateResult update() {
    return update(RepoUpdateRequest.builder().build());
  }

  public RepoUpdateResult update(Consumer<RepoUpdateRequest.Builder> spec) {
    return ClientSupport.buildAndCall(
        RepoUpdateRequest::builder, spec, RepoUpdateRequest.Builder::build, this::update);
  }

  public RepoUpdateResult update(RepoUpdateRequest request) {
    Objects.requireNonNull(request, "request");
    return gateway.repoUpdate(request);
  }

  public RepoListResult list() {
    return gateway.repoList();
  }

  public RepoRemoveResult remove(Consumer<RepoRemoveRequest.Builder> spec) {
    return ClientSupport.buildAndCall(
        RepoRemoveRequest::builder, spec, RepoRemoveRequest.Builder::build, this::remove);
  }

  public RepoRemoveResult remove(RepoRemoveRequest request) {
    Objects.requireNonNull(request, "request");
    return gateway.repoRemove(request);
  }

  public RegistryResult registryLogin(Consumer<RegistryLoginRequest.Builder> spec) {
    return ClientSupport.buildAndCall(
        RegistryLoginRequest::builder,
        spec,
        RegistryLoginRequest.Builder::build,
        this::registryLogin);
  }

  public RegistryResult registryLogin(RegistryLoginRequest request) {
    Objects.requireNonNull(request, "request");
    return gateway.registryLogin(request);
  }

  public RegistryResult registryLogout(Consumer<RegistryLogoutRequest.Builder> spec) {
    return ClientSupport.buildAndCall(
        RegistryLogoutRequest::builder,
        spec,
        RegistryLogoutRequest.Builder::build,
        this::registryLogout);
  }

  public RegistryResult registryLogout(RegistryLogoutRequest request) {
    Objects.requireNonNull(request, "request");
    return gateway.registryLogout(request);
  }
}
