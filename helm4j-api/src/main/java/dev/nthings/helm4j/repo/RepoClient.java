package dev.nthings.helm4j.repo;

import java.util.function.Consumer;

import dev.nthings.helm4j.internal.api.NamespaceClient;
import dev.nthings.helm4j.internal.gateway.RepoGateway;
import dev.nthings.helm4j.model.ListResult;

/** Repository and registry namespace for Helm SDK operations. */
public final class RepoClient extends NamespaceClient<RepoGateway> {

  public RepoClient(RepoGateway gateway) {
    super(gateway);
  }

  public RepoAddResult add(Consumer<RepoAddRequest.Builder> spec) {
    return buildAndInvoke(RepoAddRequest::builder, spec, RepoAddRequest.Builder::build, this::add);
  }

  public RepoAddResult add(RepoAddRequest request) {
    return invoke(request, gateway::repoAdd);
  }

  public ListResult<RepoUpdateEntry> update() {
    return update(RepoUpdateRequest.builder().build());
  }

  public ListResult<RepoUpdateEntry> update(Consumer<RepoUpdateRequest.Builder> spec) {
    return buildAndInvoke(
        RepoUpdateRequest::builder, spec, RepoUpdateRequest.Builder::build, this::update);
  }

  public ListResult<RepoUpdateEntry> update(RepoUpdateRequest request) {
    return invoke(request, gateway::repoUpdate);
  }

  public ListResult<RepoSummary> list() {
    return gateway.repoList();
  }

  public ListResult<String> remove(Consumer<RepoRemoveRequest.Builder> spec) {
    return buildAndInvoke(
        RepoRemoveRequest::builder, spec, RepoRemoveRequest.Builder::build, this::remove);
  }

  public ListResult<String> remove(RepoRemoveRequest request) {
    return invoke(request, gateway::repoRemove);
  }

  public RegistryResult registryLogin(Consumer<RegistryLoginRequest.Builder> spec) {
    return buildAndInvoke(
        RegistryLoginRequest::builder,
        spec,
        RegistryLoginRequest.Builder::build,
        this::registryLogin);
  }

  public RegistryResult registryLogin(RegistryLoginRequest request) {
    return invoke(request, gateway::registryLogin);
  }

  public RegistryResult registryLogout(Consumer<RegistryLogoutRequest.Builder> spec) {
    return buildAndInvoke(
        RegistryLogoutRequest::builder,
        spec,
        RegistryLogoutRequest.Builder::build,
        this::registryLogout);
  }

  public RegistryResult registryLogout(RegistryLogoutRequest request) {
    return invoke(request, gateway::registryLogout);
  }
}
