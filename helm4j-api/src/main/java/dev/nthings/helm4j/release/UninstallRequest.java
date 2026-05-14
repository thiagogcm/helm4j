package dev.nthings.helm4j.release;

import java.time.Duration;

import dev.nthings.helm4j.internal.api.Invocations;
import dev.nthings.helm4j.internal.gateway.ReleaseGateway;
import dev.nthings.helm4j.internal.model.ModelSupport;

import org.jspecify.annotations.Nullable;

/** Request parameters for uninstalling a release. */
public record UninstallRequest(
    @Nullable String releaseName,
    @Nullable String namespace,
    boolean dryRun,
    boolean disableHooks,
    boolean keepHistory,
    boolean ignoreNotFound,
    @Nullable Duration timeout,
    @Nullable String description,
    @Nullable WaitMode waitMode,
    @Nullable String deletionPropagation) {

  public UninstallRequest {
    releaseName = ModelSupport.normalizeBlankToNull(releaseName);
    namespace = ModelSupport.normalizeBlankToNull(namespace);
    description = ModelSupport.normalizeBlankToNull(description);
    deletionPropagation = ModelSupport.normalizeBlankToNull(deletionPropagation);
  }

  public static Builder builder() {
    return new Builder(null);
  }

  static Builder builder(ReleaseGateway gateway) {
    return new Builder(gateway);
  }

  public static final class Builder {
    private final ReleaseGateway gateway;
    private String releaseName;
    private String namespace;
    private boolean dryRun;
    private boolean disableHooks;
    private boolean keepHistory;
    private boolean ignoreNotFound;
    private Duration timeout;
    private String description;
    private WaitMode waitMode;
    private String deletionPropagation;

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

    public Builder dryRun(boolean value) {
      this.dryRun = value;
      return this;
    }

    public Builder disableHooks(boolean value) {
      this.disableHooks = value;
      return this;
    }

    public Builder keepHistory(boolean value) {
      this.keepHistory = value;
      return this;
    }

    public Builder ignoreNotFound(boolean value) {
      this.ignoreNotFound = value;
      return this;
    }

    public Builder timeout(Duration value) {
      this.timeout = value;
      return this;
    }

    public Builder description(String value) {
      this.description = value;
      return this;
    }

    public Builder waitMode(WaitMode value) {
      this.waitMode = value;
      return this;
    }

    public Builder deletionPropagation(String value) {
      this.deletionPropagation = value;
      return this;
    }

    public UninstallRequest build() {
      return new UninstallRequest(
          releaseName,
          namespace,
          dryRun,
          disableHooks,
          keepHistory,
          ignoreNotFound,
          timeout,
          description,
          waitMode,
          deletionPropagation);
    }

    /** Builds the request and uninstalls it through the bound client. */
    public UninstallResult execute() {
      return Invocations.requireBound(gateway).uninstall(build());
    }
  }
}
