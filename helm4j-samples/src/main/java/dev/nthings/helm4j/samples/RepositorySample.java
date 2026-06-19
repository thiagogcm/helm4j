package dev.nthings.helm4j.samples;

import dev.nthings.helm4j.samples.support.SampleOutput;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helm repository management plus a repository search: add a public repo, list configured repos,
 * refresh its index, search it, then remove it. Needs outbound network access but no cluster.
 */
public final class RepositorySample implements Sample {

  private static final Logger log = LoggerFactory.getLogger(RepositorySample.class);

  private static final String REPO_NAME = "ingress-nginx";
  private static final String REPO_URL = "https://kubernetes.github.io/ingress-nginx";

  @Override
  public String id() {
    return "repositories";
  }

  @Override
  public String title() {
    return "repositories: add, list, update, search, remove (network)";
  }

  @Override
  public Requirement requirement() {
    return Requirement.NETWORK;
  }

  @Override
  public void run(SampleContext context) {
    var helm = context.helm();
    var repositories = helm.repositories();

    log.info("Adding repository {} -> {}", REPO_NAME, REPO_URL);
    var added = repositories.add(b -> b.name(REPO_NAME).url(REPO_URL).forceUpdate(true));
    SampleOutput.printf("  added %s -> %s%n", added.name(), added.url());

    log.info("Listing configured repositories");
    repositories.list().forEach(repo -> SampleOutput.printf("  - %s %s%n", repo.name(), repo.url()));

    log.info("Refreshing repository index for {}", REPO_NAME);
    repositories
        .update(b -> b.names(REPO_NAME))
        .forEach(entry -> SampleOutput.printf("  - %s: %s%n", entry.name(), entry.status()));

    log.info("Searching repositories for {}", REPO_NAME);
    var hits = helm.charts().searchRepository(b -> b.keyword(REPO_NAME).maxColumnWidth(80));
    SampleOutput.printf("  %d hits, first 3:%n", hits.size());
    hits.stream()
        .limit(3)
        .forEach(
            chart ->
                SampleOutput.printf(
                    "    - %s %s (app %s)%n", chart.name(), chart.version(), chart.appVersion()));

    log.info("Removing repository {}", REPO_NAME);
    repositories
        .remove(b -> b.names(REPO_NAME))
        .forEach(removed -> SampleOutput.printf("  removed %s%n", removed));
  }
}
