package dev.nthings.helm4j.chart;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import dev.nthings.helm4j.internal.model.ModelSupport;

import org.jspecify.annotations.Nullable;

/** Request parameters for rendering chart templates. */
public record TemplateRequest(
    @Nullable String releaseName,
    ChartRef chart,
    ChartSource source,
    @Nullable String namespace,
    @Nullable String description,
    boolean skipCrds,
    boolean disableHooks,
    boolean disableOpenApiValidation,
    boolean generateName,
    @Nullable String nameTemplate,
    boolean subNotes,
    boolean enableDns,
    boolean includeCrds,
    List<String> apiVersions,
    Map<String, Object> values,
    Map<String, String> labels) {

  public TemplateRequest {
    releaseName = ModelSupport.normalizeBlankToNull(releaseName);
    chart = Objects.requireNonNull(chart, "chart");
    source = Objects.requireNonNullElseGet(source, ChartSource::defaults);
    namespace = ModelSupport.normalizeBlankToNull(namespace);
    description = ModelSupport.normalizeBlankToNull(description);
    nameTemplate = ModelSupport.normalizeBlankToNull(nameTemplate);
    apiVersions = ModelSupport.immutableListOrEmpty(apiVersions);
    values = ModelSupport.immutableMapOrEmpty(values);
    labels = ModelSupport.immutableMapOrEmpty(labels);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private @Nullable String releaseName;
    private @Nullable ChartRef chart;
    private @Nullable ChartSource source;
    private @Nullable String namespace;
    private @Nullable String description;
    private boolean skipCrds;
    private boolean disableHooks;
    private boolean disableOpenApiValidation;
    private boolean generateName;
    private @Nullable String nameTemplate;
    private boolean subNotes;
    private boolean enableDns;
    private boolean includeCrds;
    private @Nullable List<String> apiVersions;
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

    public Builder namespace(String value) {
      this.namespace = value;
      return this;
    }

    public Builder description(String value) {
      this.description = value;
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

    public Builder includeCrds(boolean value) {
      this.includeCrds = value;
      return this;
    }

    public Builder apiVersions(List<String> value) {
      this.apiVersions = value;
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

    public TemplateRequest build() {
      return new TemplateRequest(
          releaseName,
          Objects.requireNonNull(chart, "chart"),
          source,
          namespace,
          description,
          skipCrds,
          disableHooks,
          disableOpenApiValidation,
          generateName,
          nameTemplate,
          subNotes,
          enableDns,
          includeCrds,
          apiVersions,
          values,
          labels);
    }
  }
}
