package dev.nthings.helm4j.client.repo;

import java.util.function.Consumer;

import dev.nthings.helm4j.client.internal.NamespaceClient;
import dev.nthings.helm4j.model.ListResult;
import dev.nthings.helm4j.repo.RegistryLoginRequest;
import dev.nthings.helm4j.repo.RegistryLogoutRequest;
import dev.nthings.helm4j.repo.RegistryResult;
import dev.nthings.helm4j.repo.RepoAddRequest;
import dev.nthings.helm4j.repo.RepoAddResult;
import dev.nthings.helm4j.repo.RepoRemoveRequest;
import dev.nthings.helm4j.repo.RepoSummary;
import dev.nthings.helm4j.repo.RepoUpdateEntry;
import dev.nthings.helm4j.repo.RepoUpdateRequest;
import dev.nthings.helm4j.spi.RepoGateway;

/**
 * Repository and registry namespace for Helm SDK operations.
 *
 * <p>Each operation has two entry points: one that takes a {@link Consumer} configuring a fluent
 * request builder, and an overload that takes a pre-built request for reuse.
 */
public final class RepoClient extends NamespaceClient<RepoGateway> {

  public RepoClient(RepoGateway gateway) {
    super(gateway);
  }

  public RepoAddResult add(Consumer<RepoAddRequest.Builder> spec) {
    return gateway.repoAdd(configured(RepoAddRequest::builder, spec).build());
  }

  public RepoAddResult add(RepoAddRequest request) {
    return gateway.repoAdd(request);
  }

  public ListResult<RepoUpdateEntry> update(Consumer<RepoUpdateRequest.Builder> spec) {
    return gateway.repoUpdate(configured(RepoUpdateRequest::builder, spec).build());
  }

  public ListResult<RepoUpdateEntry> update(RepoUpdateRequest request) {
    return gateway.repoUpdate(request);
  }

  /** Lists the configured chart repositories. */
  public ListResult<RepoSummary> list() {
    return gateway.repoList();
  }

  public ListResult<String> remove(Consumer<RepoRemoveRequest.Builder> spec) {
    return gateway.repoRemove(configured(RepoRemoveRequest::builder, spec).build());
  }

  public ListResult<String> remove(RepoRemoveRequest request) {
    return gateway.repoRemove(request);
  }

  public RegistryResult registryLogin(Consumer<RegistryLoginRequest.Builder> spec) {
    return gateway.registryLogin(configured(RegistryLoginRequest::builder, spec).build());
  }

  public RegistryResult registryLogin(RegistryLoginRequest request) {
    return gateway.registryLogin(request);
  }

  public RegistryResult registryLogout(Consumer<RegistryLogoutRequest.Builder> spec) {
    return gateway.registryLogout(configured(RegistryLogoutRequest::builder, spec).build());
  }

  public RegistryResult registryLogout(RegistryLogoutRequest request) {
    return gateway.registryLogout(request);
  }
}
