package dev.nthings.helm4j.repo;

/** Domain result for repository add operations. */
public sealed interface RepoAddResult permits RepoAddSuccess, RepoAddFailure {}
