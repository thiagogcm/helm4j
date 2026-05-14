package dev.nthings.helm4j.repo;

import dev.nthings.helm4j.internal.api.NamespaceClient;
import dev.nthings.helm4j.internal.gateway.RepoGateway;
import dev.nthings.helm4j.model.ListResult;

/**
 * Repository and registry namespace for Helm SDK operations.
 *
 * <p>Each operation has two entry points: a method that returns a runnable, fluent request builder
 * (call {@code execute()} on it), and an overload that takes a pre-built request for reuse.
 */
public final class RepoClient extends NamespaceClient<RepoGateway> {

  public RepoClient(RepoGateway gateway) {
    super(gateway);
  }

  /** Begins a fluent repository add; call {@code execute()} to run it. */
  public RepoAddRequest.Builder add() {
    return RepoAddRequest.builder(gateway);
  }

  public RepoAddResult add(RepoAddRequest request) {
    return gateway.repoAdd(request);
  }

  /** Begins a fluent repository update; call {@code execute()} to run it. */
  public RepoUpdateRequest.Builder update() {
    return RepoUpdateRequest.builder(gateway);
  }

  public ListResult<RepoUpdateEntry> update(RepoUpdateRequest request) {
    return gateway.repoUpdate(request);
  }

  /** Lists the configured chart repositories. */
  public ListResult<RepoSummary> list() {
    return gateway.repoList();
  }

  /** Begins a fluent repository removal; call {@code execute()} to run it. */
  public RepoRemoveRequest.Builder remove() {
    return RepoRemoveRequest.builder(gateway);
  }

  public ListResult<String> remove(RepoRemoveRequest request) {
    return gateway.repoRemove(request);
  }

  /** Begins a fluent registry login; call {@code execute()} to run it. */
  public RegistryLoginRequest.Builder registryLogin() {
    return RegistryLoginRequest.builder(gateway);
  }

  public RegistryResult registryLogin(RegistryLoginRequest request) {
    return gateway.registryLogin(request);
  }

  /** Begins a fluent registry logout; call {@code execute()} to run it. */
  public RegistryLogoutRequest.Builder registryLogout() {
    return RegistryLogoutRequest.builder(gateway);
  }

  public RegistryResult registryLogout(RegistryLogoutRequest request) {
    return gateway.registryLogout(request);
  }
}
