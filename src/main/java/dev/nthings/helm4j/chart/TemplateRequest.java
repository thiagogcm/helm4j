package dev.nthings.helm4j.chart;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import dev.nthings.helm4j.types.ChartRef;
import dev.nthings.helm4j.types.ChartSource;

/** Request parameters for rendering chart templates. */
public record TemplateRequest(
    String releaseName,
    ChartRef chart,
    ChartSource source,
    String namespace,
    String description,
    boolean skipCrds,
    boolean disableHooks,
    boolean disableOpenApiValidation,
    boolean generateName,
    String nameTemplate,
    boolean subNotes,
    boolean enableDns,
    boolean includeCrds,
    List<String> apiVersions,
    Map<String, Object> values,
    Map<String, String> labels) {

  public TemplateRequest {
    releaseName = normalize(releaseName);
    chart = Objects.requireNonNull(chart, "chart");
    source = Objects.requireNonNullElseGet(source, ChartSource::defaults);
    namespace = normalize(namespace);
    description = normalize(description);
    nameTemplate = normalize(nameTemplate);
    apiVersions = apiVersions == null ? List.of() : List.copyOf(apiVersions);
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
    private String description;
    private boolean skipCrds;
    private boolean disableHooks;
    private boolean disableOpenApiValidation;
    private boolean generateName;
    private String nameTemplate;
    private boolean subNotes;
    private boolean enableDns;
    private boolean includeCrds;
    private List<String> apiVersions;
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
      var resolvedSource = source != null ? source.merge(sourceBuilder.build()) : sourceBuilder.build();
      return new TemplateRequest(
          releaseName,
          chart,
          resolvedSource,
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
