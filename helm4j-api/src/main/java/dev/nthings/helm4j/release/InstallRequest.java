package dev.nthings.helm4j.release;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import dev.nthings.helm4j.chart.ChartRef;
import dev.nthings.helm4j.chart.ChartSource;
import dev.nthings.helm4j.internal.model.ModelSupport;

import org.jspecify.annotations.Nullable;

/** Request parameters for installing a release from a chart reference. */
public record InstallRequest(
    @Nullable String releaseName,
    ChartRef chart,
    ChartSource source,
    @Nullable String namespace,
    boolean createNamespace,
    @Nullable DryRunMode dryRun,
    @Nullable WaitMode waitMode,
    boolean waitForJobs,
    @Nullable Duration timeout,
    @Nullable String description,
    boolean rollbackOnFailure,
    boolean skipCrds,
    boolean disableHooks,
    boolean disableOpenApiValidation,
    boolean forceReplace,
    boolean replace,
    boolean generateName,
    @Nullable String nameTemplate,
    boolean subNotes,
    boolean enableDns,
    boolean takeOwnership,
    boolean dependencyUpdate,
    ApplyStrategy applyStrategy,
    Map<String, Object> values,
    Map<String, String> labels) {

  public InstallRequest {
    releaseName = ModelSupport.normalizeBlankToNull(releaseName);
    chart = Objects.requireNonNull(chart, "chart");
    source = Objects.requireNonNullElseGet(source, ChartSource::defaults);
    namespace = ModelSupport.normalizeBlankToNull(namespace);
    description = ModelSupport.normalizeBlankToNull(description);
    nameTemplate = ModelSupport.normalizeBlankToNull(nameTemplate);
    applyStrategy = Objects.requireNonNullElse(applyStrategy, ApplyStrategy.SERVER_SIDE_APPLY);
    values = ModelSupport.immutableMapOrEmpty(values);
    labels = ModelSupport.immutableMapOrEmpty(labels);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private final ChartSource.Builder sourceBuilder = ChartSource.builder();
    private @Nullable String releaseName;
    private @Nullable ChartRef chart;
    private @Nullable ChartSource source;
    private @Nullable String namespace;
    private boolean createNamespace;
    private @Nullable DryRunMode dryRun;
    private @Nullable WaitMode waitMode;
    private boolean waitForJobs;
    private @Nullable Duration timeout;
    private @Nullable String description;
    private boolean rollbackOnFailure;
    private boolean skipCrds;
    private boolean disableHooks;
    private boolean disableOpenApiValidation;
    private boolean forceReplace;
    private boolean replace;
    private boolean generateName;
    private @Nullable String nameTemplate;
    private boolean subNotes;
    private boolean enableDns;
    private boolean takeOwnership;
    private boolean dependencyUpdate;
    private ApplyStrategy applyStrategy = ApplyStrategy.SERVER_SIDE_APPLY;
    private @Nullable Map<String, Object> values;
    private @Nullable Map<String, String> labels;

    private Builder() {}

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

    public Builder createNamespace(boolean value) {
      this.createNamespace = value;
      return this;
    }

    public Builder dryRun(DryRunMode value) {
      this.dryRun = value;
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

    public Builder replace(boolean value) {
      this.replace = value;
      return this;
    }

    public Builder generateName(boolean value) {
      this.generateName = value;
      return this;
    }

    public Builder nameTemplate(String value) {
      this.nameTemplate = value;
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

    public Builder dependencyUpdate(boolean value) {
      this.dependencyUpdate = value;
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

    public InstallRequest build() {
      var resolvedSource =
          source != null ? source.merge(sourceBuilder.build()) : sourceBuilder.build();
      return new InstallRequest(
          releaseName,
          Objects.requireNonNull(chart, "chart"),
          resolvedSource,
          namespace,
          createNamespace,
          dryRun,
          waitMode,
          waitForJobs,
          timeout,
          description,
          rollbackOnFailure,
          skipCrds,
          disableHooks,
          disableOpenApiValidation,
          forceReplace,
          replace,
          generateName,
          nameTemplate,
          subNotes,
          enableDns,
          takeOwnership,
          dependencyUpdate,
          applyStrategy,
          ModelSupport.immutableMapOrEmpty(values),
          ModelSupport.immutableMapOrEmpty(labels));
    }
  }
}
