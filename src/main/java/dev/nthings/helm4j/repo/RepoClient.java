package dev.nthings.helm4j.repo;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import dev.nthings.helm4j.internal.sdk.HelmGateway;

/** Repository and registry namespace for Helm SDK operations. */
public final class RepoClient {

  private final HelmGateway gateway;

  public RepoClient(HelmGateway gateway) {
    this.gateway = Objects.requireNonNull(gateway, "gateway");
  }

  public RepoAddResult add(String name, String url) {
    return add(spec -> spec.name(name).url(url));
  }

  public RepoAddResult add(Consumer<RepoAddRequest.Builder> spec) {
    Objects.requireNonNull(spec, "spec");
    var builder = RepoAddRequest.builder();
    spec.accept(builder);
    return add(builder.build());
  }

  public RepoAddResult add(RepoAddRequest request) {
    Objects.requireNonNull(request, "request");
    return gateway.repoAdd(request);
  }

  public RepoUpdateResult update() {
    return update(RepoUpdateRequest.defaults());
  }

  public RepoUpdateResult update(Consumer<RepoUpdateRequest.Builder> spec) {
    Objects.requireNonNull(spec, "spec");
    var builder = RepoUpdateRequest.builder();
    spec.accept(builder);
    return update(builder.build());
  }

  public RepoUpdateResult update(RepoUpdateRequest request) {
    Objects.requireNonNull(request, "request");
    return gateway.repoUpdate(request);
  }

  public RepoListResult list() {
    return gateway.repoList();
  }

  public RepoRemoveResult remove(String... names) {
    Objects.requireNonNull(names, "names");
    return remove(spec -> spec.names(names));
  }

  public RepoRemoveResult remove(Consumer<RepoRemoveRequest.Builder> spec) {
    Objects.requireNonNull(spec, "spec");
    var builder = RepoRemoveRequest.builder();
    spec.accept(builder);
    return remove(builder.build());
  }

  public RepoRemoveResult remove(RepoRemoveRequest request) {
    Objects.requireNonNull(request, "request");
    var normalized = new RepoRemoveRequest(List.copyOf(request.names()));
    return gateway.repoRemove(normalized);
  }

  public RegistryResult registryLogin(String hostname, String username, String password) {
    return registryLogin(
        RegistryLoginRequest.builder()
            .hostname(hostname)
            .username(username)
            .password(password)
            .build());
  }

  public RegistryResult registryLogin(Consumer<RegistryLoginRequest.Builder> spec) {
    Objects.requireNonNull(spec, "spec");
    var builder = RegistryLoginRequest.builder();
    spec.accept(builder);
    return registryLogin(builder.build());
  }

  public RegistryResult registryLogin(RegistryLoginRequest request) {
    Objects.requireNonNull(request, "request");
    return gateway.registryLogin(request);
  }

  public RegistryResult registryLogout(String hostname) {
    return registryLogout(RegistryLogoutRequest.builder().hostname(hostname).build());
  }

  public RegistryResult registryLogout(Consumer<RegistryLogoutRequest.Builder> spec) {
    Objects.requireNonNull(spec, "spec");
    var builder = RegistryLogoutRequest.builder();
    spec.accept(builder);
    return registryLogout(builder.build());
  }

  public RegistryResult registryLogout(RegistryLogoutRequest request) {
    Objects.requireNonNull(request, "request");
    return gateway.registryLogout(request);
  }
}
