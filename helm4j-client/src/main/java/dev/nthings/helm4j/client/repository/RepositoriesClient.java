package dev.nthings.helm4j.client.repository;

import java.util.function.Consumer;

import dev.nthings.helm4j.client.internal.NamespaceClient;
import dev.nthings.helm4j.model.ListResult;
import dev.nthings.helm4j.repository.AddRepository;
import dev.nthings.helm4j.repository.AddRepositoryReport;
import dev.nthings.helm4j.repository.RemoveRepository;
import dev.nthings.helm4j.repository.RepositorySummary;
import dev.nthings.helm4j.repository.RepositoryUpdateEntry;
import dev.nthings.helm4j.repository.UpdateRepositories;
import dev.nthings.helm4j.spi.RepositoryGateway;

/**
 * Repositories namespace: add/update/list/remove Helm chart repositories. OCI registry login lives
 * on {@code helm.registries()}.
 */
public final class RepositoriesClient extends NamespaceClient<RepositoryGateway> {

  public RepositoriesClient(RepositoryGateway gateway) {
    super(gateway);
  }

  public AddRepositoryReport add(Consumer<AddRepository.Builder> spec) {
    return gateway.add(configured(AddRepository::builder, spec).build());
  }

  public AddRepositoryReport add(AddRepository request) {
    return gateway.add(request);
  }

  public ListResult<RepositoryUpdateEntry> update(Consumer<UpdateRepositories.Builder> spec) {
    return gateway.update(configured(UpdateRepositories::builder, spec).build());
  }

  public ListResult<RepositoryUpdateEntry> update(UpdateRepositories request) {
    return gateway.update(request);
  }

  /** Lists the configured chart repositories. */
  public ListResult<RepositorySummary> list() {
    return gateway.list();
  }

  public ListResult<String> remove(Consumer<RemoveRepository.Builder> spec) {
    return gateway.remove(configured(RemoveRepository::builder, spec).build());
  }

  public ListResult<String> remove(RemoveRepository request) {
    return gateway.remove(request);
  }
}
