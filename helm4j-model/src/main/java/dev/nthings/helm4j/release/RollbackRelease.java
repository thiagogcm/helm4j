package dev.nthings.helm4j.release;

import java.time.Duration;
import java.util.Objects;

import dev.nthings.helm4j.internal.model.ModelSupport;

import org.jspecify.annotations.Nullable;

/** Request parameters for rolling back a release. */
public record RollbackRelease(
    @Nullable String releaseName,
    @Nullable String namespace,
    int revision,
    @Nullable DryRunMode dryRun,
    @Nullable Duration timeout,
    @Nullable WaitMode waitMode,
    boolean waitForJobs,
    boolean disableHooks,
    boolean forceReplace,
    boolean cleanupOnFail,
    int maxHistory,
    ApplyStrategy applyStrategy) {

  public RollbackRelease {
    releaseName = ModelSupport.normalizeBlankToNull(releaseName);
    namespace = ModelSupport.normalizeBlankToNull(namespace);
    applyStrategy = Objects.requireNonNullElse(applyStrategy, ApplyStrategy.SERVER_SIDE_APPLY);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private @Nullable String releaseName;
    private @Nullable String namespace;
    private int revision;
    private @Nullable DryRunMode dryRun;
    private @Nullable Duration timeout;
    private @Nullable WaitMode waitMode;
    private boolean waitForJobs;
    private boolean disableHooks;
    private boolean forceReplace;
    private boolean cleanupOnFail;
    private int maxHistory;
    private ApplyStrategy applyStrategy = ApplyStrategy.SERVER_SIDE_APPLY;

    private Builder() {}

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

    public Builder dryRun(DryRunMode value) {
      this.dryRun = value;
      return this;
    }

    public Builder timeout(Duration value) {
      this.timeout = value;
      return this;
    }

    public Builder waitMode(WaitMode value) {
      this.waitMode = value;
      return this;
    }

    public Builder waitForJobs(boolean value) {
      this.waitForJobs = value;
      return this;
    }

    public Builder disableHooks(boolean value) {
      this.disableHooks = value;
      return this;
    }

    public Builder forceReplace(boolean value) {
      this.forceReplace = value;
      return this;
    }

    public Builder cleanupOnFail(boolean value) {
      this.cleanupOnFail = value;
      return this;
    }

    public Builder maxHistory(int value) {
      this.maxHistory = value;
      return this;
    }

    public Builder applyStrategy(ApplyStrategy value) {
      this.applyStrategy = value;
      return this;
    }

    public RollbackRelease build() {
      return new RollbackRelease(
          releaseName,
          namespace,
          revision,
          dryRun,
          timeout,
          waitMode,
          waitForJobs,
          disableHooks,
          forceReplace,
          cleanupOnFail,
          maxHistory,
          applyStrategy);
    }
  }
}
