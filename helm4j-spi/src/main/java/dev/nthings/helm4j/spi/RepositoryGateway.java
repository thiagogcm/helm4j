package dev.nthings.helm4j.spi;

import dev.nthings.helm4j.model.ListResult;
import dev.nthings.helm4j.repository.AddRepository;
import dev.nthings.helm4j.repository.AddRepositoryReport;
import dev.nthings.helm4j.repository.RemoveRepository;
import dev.nthings.helm4j.repository.RepositorySummary;
import dev.nthings.helm4j.repository.RepositoryUpdateEntry;
import dev.nthings.helm4j.repository.UpdateRepositories;

/** SPI for Helm chart repository operations. */
public interface RepositoryGateway {

  AddRepositoryReport add(AddRepository request);

  ListResult<RepositoryUpdateEntry> update(UpdateRepositories request);

  ListResult<RepositorySummary> list();

  ListResult<String> remove(RemoveRepository request);
}
