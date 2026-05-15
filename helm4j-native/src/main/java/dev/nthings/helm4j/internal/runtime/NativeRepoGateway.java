package dev.nthings.helm4j.internal.runtime;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import dev.nthings.helm4j.errors.HelmException;
import dev.nthings.helm4j.model.ListResult;
import dev.nthings.helm4j.repo.RegistryLoginRequest;
import dev.nthings.helm4j.repo.RegistryLogoutRequest;
import dev.nthings.helm4j.repo.RegistryResult;
import dev.nthings.helm4j.repo.RepoAddFailure;
import dev.nthings.helm4j.repo.RepoAddRequest;
import dev.nthings.helm4j.repo.RepoAddResult;
import dev.nthings.helm4j.repo.RepoAddSuccess;
import dev.nthings.helm4j.repo.RepoRemoveRequest;
import dev.nthings.helm4j.repo.RepoSummary;
import dev.nthings.helm4j.repo.RepoUpdateEntry;
import dev.nthings.helm4j.repo.RepoUpdateRequest;
import dev.nthings.helm4j.spi.RepoGateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dev.nthings.helm4j.internal.runtime.NativeGatewaySupport.failure;
import static dev.nthings.helm4j.internal.runtime.NativeGatewaySupport.listOrEmpty;
import static dev.nthings.helm4j.internal.runtime.NativeGatewaySupport.operationError;
import static dev.nthings.helm4j.internal.runtime.NativeGatewaySupport.utf8;

/** Repository and registry operations backed by the JSON bridge. */
final class NativeRepoGateway implements RepoGateway {

  private static final Logger log = LoggerFactory.getLogger(NativeRepoGateway.class);

  private final NativeGatewaySupport support;

  NativeRepoGateway(NativeGatewaySupport support) {
    this.support = Objects.requireNonNull(support, "support");
  }

  @Override
  public RepoAddResult repoAdd(RepoAddRequest request) {
    Objects.requireNonNull(request, "request");
    if (request.name() == null || request.url() == null) {
      throw new IllegalArgumentException("Repository add requires non-null name and url");
    }

    log.debug("Adding repository: name={}, url={}", request.name(), request.url());
    var root =
        support.invokeRoot(
            "repo add",
            bridge ->
                bridge.repo(
                    utf8("add"), support.toJsonBytes(NativeOptions.repoAdd(request), "repo add")));

    var error = operationError(root, "repo add");
    if (error != null) {
      return new RepoAddFailure(failure(error));
    }

    var response = support.convert(root, RepoAddPayload.class, "repo add");
    if (response == null || response.name() == null || response.url() == null) {
      throw new HelmException(
          "Native repo add response missing required fields", "decodeResponse", "repo add");
    }
    return new RepoAddSuccess(response.name(), response.url());
  }

  @Override
  public ListResult<RepoUpdateEntry> repoUpdate(RepoUpdateRequest request) {
    Objects.requireNonNull(request, "request");

    log.debug("Updating repositories: names={}", request.names());
    var root =
        support.invokeRootOrThrow(
            "repo update",
            bridge ->
                bridge.repo(
                    utf8("update"),
                    support.toJsonBytes(NativeOptions.repoUpdate(request), "repo update")));

    var response = support.convert(root, RepoUpdatePayload.class, "repo update");
    var repositories =
        listOrEmpty(response == null ? null : response.repositories()).stream()
            .map(entry -> new RepoUpdateEntry(entry.name(), entry.status()))
            .toList();
    return ListResult.of(repositories);
  }

  @Override
  public ListResult<RepoSummary> repoList() {
    log.debug("Listing repositories");
    var root =
        support.invokeRootOrThrow(
            "repo list",
            bridge -> bridge.repo(utf8("list"), support.toJsonBytes(Map.of(), "repo list")));

    var response = support.convert(root, RepoListPayload.class, "repo list");
    var repositories =
        listOrEmpty(response == null ? null : response.repositories()).stream()
            .map(entry -> new RepoSummary(entry.name(), entry.url()))
            .toList();
    return ListResult.of(repositories);
  }

  @Override
  public ListResult<String> repoRemove(RepoRemoveRequest request) {
    Objects.requireNonNull(request, "request");

    log.debug("Removing repositories: names={}", request.names());
    var root =
        support.invokeRootOrThrow(
            "repo remove",
            bridge ->
                bridge.repo(
                    utf8("remove"),
                    support.toJsonBytes(NativeOptions.repoRemove(request), "repo remove")));

    var response = support.convert(root, RepoRemovePayload.class, "repo remove");
    return ListResult.of(listOrEmpty(response == null ? null : response.removed()));
  }

  @Override
  public RegistryResult registryLogin(RegistryLoginRequest request) {
    Objects.requireNonNull(request, "request");
    if (request.hostname() == null) {
      throw new IllegalArgumentException("Registry login requires hostname");
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

    var response = support.convert(root, RegistryPayload.class, "registry login");
    if (response == null) {
      throw new HelmException(
          "Native registry login response missing data", "decodeResponse", "registry login");
    }
    return new RegistryResult(response.hostname(), response.status());
  }

  @Override
  public RegistryResult registryLogout(RegistryLogoutRequest request) {
    Objects.requireNonNull(request, "request");
    if (request.hostname() == null) {
      throw new IllegalArgumentException("Registry logout requires hostname");
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

    var response = support.convert(root, RegistryPayload.class, "registry logout");
    if (response == null) {
      throw new HelmException(
          "Native registry logout response missing data", "decodeResponse", "registry logout");
    }
    return new RegistryResult(response.hostname(), response.status());
  }

  private record RepoAddPayload(String name, String url) {}

  private record RepoUpdatePayload(List<RepoUpdateEntryPayload> repositories) {}

  private record RepoUpdateEntryPayload(String name, String status) {}

  private record RepoListPayload(List<RepoListEntryPayload> repositories) {}

  private record RepoListEntryPayload(String name, String url) {}

  private record RepoRemovePayload(List<String> removed) {}

  private record RegistryPayload(String hostname, String status) {}
}
