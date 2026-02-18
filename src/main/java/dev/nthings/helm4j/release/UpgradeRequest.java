package dev.nthings.helm4j.release;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import dev.nthings.helm4j.types.ChartRef;
import dev.nthings.helm4j.types.ChartSource;

/** Request parameters for upgrading an existing release. */
public record UpgradeRequest(
    String releaseName,
    ChartRef chart,
    ChartSource source,
    String namespace,
    boolean install,
    DryRunMode dryRunMode,
    WaitMode waitMode,
    boolean waitForJobs,
    Duration timeout,
    String description,
    boolean rollbackOnFailure,
    boolean skipCrds,
    boolean disableHooks,
    boolean disableOpenApiValidation,
    boolean forceReplace,
    boolean subNotes,
    boolean enableDns,
    boolean takeOwnership,
    boolean cleanupOnFail,
    int maxHistory,
    boolean reuseValues,
    boolean resetValues,
    boolean resetThenReuseValues,
    ApplyStrategy applyStrategy,
    Map<String, Object> values,
    Map<String, String> labels) {

  public UpgradeRequest {
    releaseName = normalize(releaseName);
    chart = Objects.requireNonNull(chart, "chart");
    source = Objects.requireNonNullElseGet(source, ChartSource::defaults);
    namespace = normalize(namespace);
    description = normalize(description);
    applyStrategy = Objects.requireNonNullElse(applyStrategy, ApplyStrategy.SERVER_SIDE_APPLY);
    values = copyMap(values);
    labels = copyMap(labels);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private final ChartSource.Builder sourceBuilder = ChartSource.builder();
    private String releaseName;
    private ChartRef chart;
    private ChartSource source;
    private String namespace;
    private boolean install;
    private DryRunMode dryRunMode;
    private WaitMode waitMode;
    private boolean waitForJobs;
    private Duration timeout;
    private String description;
    private boolean rollbackOnFailure;
    private boolean skipCrds;
    private boolean disableHooks;
    private boolean disableOpenApiValidation;
    private boolean forceReplace;
    private boolean subNotes;
    private boolean enableDns;
    private boolean takeOwnership;
    private boolean cleanupOnFail;
    private int maxHistory;
    private boolean reuseValues;
    private boolean resetValues;
    private boolean resetThenReuseValues;
    private ApplyStrategy applyStrategy = ApplyStrategy.SERVER_SIDE_APPLY;
    private Map<String, Object> values;
    private Map<String, String> labels;

    private Builder() {
    }

    public Builder releaseName(String value) {
      this.releaseName = value;
      return this;
    }

    public Builder chart(ChartRef value) {
      this.chart = value;
      return this;
    }

    public Builder source(ChartSource value) {
      this.source = value;
      return this;
    }

    public Builder source(Consumer<ChartSource.Builder> consumer) {
      consumer.accept(sourceBuilder);
      return this;
    }

    public Builder namespace(String value) {
      this.namespace = value;
      return this;
    }

    public Builder install(boolean value) {
      this.install = value;
      return this;
    }

    public Builder dryRunMode(DryRunMode value) {
      this.dryRunMode = value;
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

    public Builder timeout(Duration value) {
      this.timeout = value;
      return this;
    }

    public Builder description(String value) {
      this.description = value;
      return this;
    }

    public Builder rollbackOnFailure(boolean value) {
      this.rollbackOnFailure = value;
      return this;
    }

    public Builder skipCrds(boolean value) {
      this.skipCrds = value;
      return this;
    }

    public Builder disableHooks(boolean value) {
      this.disableHooks = value;
      return this;
    }

    public Builder disableOpenApiValidation(boolean value) {
      this.disableOpenApiValidation = value;
      return this;
    }

    public Builder forceReplace(boolean value) {
      this.forceReplace = value;
      return this;
    }

    public Builder subNotes(boolean value) {
      this.subNotes = value;
      return this;
    }

    public Builder enableDns(boolean value) {
      this.enableDns = value;
      return this;
    }

    public Builder takeOwnership(boolean value) {
      this.takeOwnership = value;
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

    public Builder reuseValues(boolean value) {
      this.reuseValues = value;
      return this;
    }

    public Builder resetValues(boolean value) {
      this.resetValues = value;
      return this;
    }

    public Builder resetThenReuseValues(boolean value) {
      this.resetThenReuseValues = value;
      return this;
    }

    public Builder applyStrategy(ApplyStrategy value) {
      this.applyStrategy = value;
      return this;
    }

    public Builder values(Map<String, Object> value) {
      this.values = value;
      return this;
    }

    public Builder labels(Map<String, String> value) {
      this.labels = value;
      return this;
    }

    public UpgradeRequest build() {
      var resolvedSource = source != null ? source.merge(sourceBuilder.build()) : sourceBuilder.build();
      return new UpgradeRequest(
          releaseName,
          chart,
          resolvedSource,
          namespace,
          install,
          dryRunMode,
          waitMode,
          waitForJobs,
          timeout,
          description,
          rollbackOnFailure,
          skipCrds,
          disableHooks,
          disableOpenApiValidation,
          forceReplace,
          subNotes,
          enableDns,
          takeOwnership,
          cleanupOnFail,
          maxHistory,
          reuseValues,
          resetValues,
          resetThenReuseValues,
          applyStrategy,
          values,
          labels);
    }
  }

  private static String normalize(String value) {
    if (value == null)
      return null;
    var normalized = value.trim();
    return normalized.isEmpty() ? null : normalized;
  }

  private static <T> Map<String, T> copyMap(Map<String, T> value) {
    if (value == null || value.isEmpty())
      return Map.of();
    return Map.copyOf(new LinkedHashMap<>(value));
  }
}
