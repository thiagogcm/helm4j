package dev.nthings.helm4j.release;

import dev.nthings.helm4j.internal.api.Invocations;
import dev.nthings.helm4j.internal.gateway.ReleaseGateway;
import dev.nthings.helm4j.internal.model.ModelSupport;

/** Request parameters for getting release information. */
public record GetRequest(String releaseName, String namespace, int revision, boolean allValues) {

  public GetRequest {
    releaseName = ModelSupport.normalizeBlankToNull(releaseName);
    namespace = ModelSupport.normalizeBlankToNull(namespace);
  }

  public static Builder builder() {
    return new Builder(null);
  }

  static Builder builder(ReleaseGateway gateway) {
    return new Builder(gateway);
  }

  /**
   * Fluent builder for the release {@code get} family. The same request shape feeds every variant,
   * so the variant is chosen by the terminal method ({@link #all()}, {@link #values()}, ...).
   */
  public static final class Builder {
    private final ReleaseGateway gateway;
    private String releaseName;
    private String namespace;
    private int revision;
    private boolean allValues;

    private Builder(ReleaseGateway gateway) {
      this.gateway = gateway;
    }

    public Builder releaseName(String value) {
      this.releaseName = value;
      return this;
    }

    public Builder namespace(String value) {
      this.namespace = value;
      return this;
    }

    public Builder revision(int value) {
      this.revision = value;
      return this;
    }

    public Builder allValues(boolean value) {
      this.allValues = value;
      return this;
    }

    public GetRequest build() {
      return new GetRequest(releaseName, namespace, revision, allValues);
    }

    /** Fetches the release metadata, values, manifest, hooks and notes in one call. */
    public GetAllResult all() {
      return Invocations.requireBound(gateway).getAll(build());
    }

    /** Fetches the supplied (or computed) values of the release. */
    public GetValuesResult values() {
      return Invocations.requireBound(gateway).getValues(build());
    }

    /** Fetches the rendered manifest of the release. */
    public GetManifestResult manifest() {
      return Invocations.requireBound(gateway).getManifest(build());
    }

    /** Fetches the hooks of the release. */
    public GetHooksResult hooks() {
      return Invocations.requireBound(gateway).getHooks(build());
    }

    /** Fetches the notes of the release. */
    public GetNotesResult notes() {
      return Invocations.requireBound(gateway).getNotes(build());
    }

    /** Fetches the metadata of the release. */
    public GetMetadataResult metadata() {
      return Invocations.requireBound(gateway).getMetadata(build());
    }
  }
}
