package dev.nthings.helm4j.samples;

/**
 * Declares what a {@link Sample} needs in order to run, so {@link SamplesApp} can offer a safe
 * subset (e.g. the {@code offline} group) to users without a cluster.
 */
public enum Requirement {

  /** Runs fully offline — no outbound network, no Kubernetes cluster. */
  OFFLINE,

  /** Needs outbound network access (e.g. to reach Helm chart repositories). */
  NETWORK,

  /** Needs a reachable Kubernetes cluster. */
  CLUSTER
}
