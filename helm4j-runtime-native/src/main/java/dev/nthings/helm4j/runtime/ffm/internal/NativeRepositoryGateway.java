package dev.nthings.helm4j.runtime.ffm.internal;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import dev.nthings.helm4j.errors.HelmConfigurationException;
import dev.nthings.helm4j.model.ListResult;
import dev.nthings.helm4j.repository.AddRepository;
import dev.nthings.helm4j.repository.AddRepositoryReport;
import dev.nthings.helm4j.repository.RemoveRepository;
import dev.nthings.helm4j.repository.RepositorySummary;
import dev.nthings.helm4j.repository.RepositoryUpdateEntry;
import dev.nthings.helm4j.repository.UpdateRepositories;
import dev.nthings.helm4j.spi.RepositoryGateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dev.nthings.helm4j.runtime.ffm.internal.NativeGatewaySupport.listOrEmpty;
import static dev.nthings.helm4j.runtime.ffm.internal.NativeGatewaySupport.requireResponse;
import static dev.nthings.helm4j.runtime.ffm.internal.NativeGatewaySupport.utf8;

/** Helm chart repository operations backed by the JSON bridge. */
final class NativeRepositoryGateway implements RepositoryGateway {

  private static final Logger log = LoggerFactory.getLogger(NativeRepositoryGateway.class);

  private final NativeGatewaySupport support;

  NativeRepositoryGateway(NativeGatewaySupport support) {
    this.support = Objects.requireNonNull(support, "support");
  }

  @Override
  public AddRepositoryReport add(AddRepository request) {
    Objects.requireNonNull(request, "request");
    if (request.name() == null || request.url() == null) {
      throw new HelmConfigurationException("Repository add requires non-null name and url");
    }

    log.debug("Adding repository: name={}, url={}", request.name(), request.url());
    var root =
        support.invokeRootOrThrow(
            "repo add",
            bridge ->
                bridge.repo(
                    utf8("add"), support.toJsonBytes(NativeOptions.repoAdd(request), "repo add")));

    var response =
        requireResponse(
            support.convert(root, RepoAddPayload.class, "repo add"), "repo add", "data");
    return new AddRepositoryReport(
        requireResponse(response.name(), "repo add", "name"),
        requireResponse(response.url(), "repo add", "url"));
  }

  @Override
  public ListResult<RepositoryUpdateEntry> update(UpdateRepositories request) {
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
            .map(entry -> new RepositoryUpdateEntry(entry.name(), entry.status()))
            .toList();
    return ListResult.of(repositories);
  }

  @Override
  public ListResult<RepositorySummary> list() {
    log.debug("Listing repositories");
    var root =
        support.invokeRootOrThrow(
            "repo list",
            bridge -> bridge.repo(utf8("list"), support.toJsonBytes(Map.of(), "repo list")));

    var response = support.convert(root, RepoListPayload.class, "repo list");
    var repositories =
        listOrEmpty(response == null ? null : response.repositories()).stream()
            .map(entry -> new RepositorySummary(entry.name(), entry.url()))
            .toList();
    return ListResult.of(repositories);
  }

  @Override
  public ListResult<String> remove(RemoveRepository request) {
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

  private record RepoAddPayload(String name, String url) {}

  private record RepoUpdatePayload(List<RepoUpdateEntryPayload> repositories) {}

  private record RepoUpdateEntryPayload(String name, String status) {}

  private record RepoListPayload(List<RepoListEntryPayload> repositories) {}

  private record RepoListEntryPayload(String name, String url) {}

  private record RepoRemovePayload(List<String> removed) {}
}
