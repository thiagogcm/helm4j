package dev.nthings.helm4j.internal.spi;

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

/** Internal repository and registry operations exposed to the repo namespace client. */
public interface RepoGateway {

  RepoAddResult repoAdd(RepoAddRequest request);

  ListResult<RepoUpdateEntry> repoUpdate(RepoUpdateRequest request);

  ListResult<RepoSummary> repoList();

  ListResult<String> repoRemove(RepoRemoveRequest request);

  RegistryResult registryLogin(RegistryLoginRequest request);

  RegistryResult registryLogout(RegistryLogoutRequest request);
}
