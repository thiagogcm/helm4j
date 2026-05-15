package dev.nthings.helm4j;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import dev.nthings.helm4j.spi.HelmEngineConfig;

import org.jspecify.annotations.Nullable;

/**
 * Options for {@link HelmClient#create(HelmClientOptions)}.
 *
 * <p>{@code runtimeId} pins the provider when more than one is on the module path. {@code
 * kubeContext} is forwarded to every supported runtime. Additional runtime-specific fields flow
 * through {@link Builder#property(String, String)}.
 */
public final class HelmClientOptions {

  private final @Nullable String runtimeId;
  private final @Nullable String kubeContext;
  private final Map<String, String> properties;

  private HelmClientOptions(
      @Nullable String runtimeId, @Nullable String kubeContext, Map<String, String> properties) {
    this.runtimeId = runtimeId;
    this.kubeContext = kubeContext;
    this.properties = Map.copyOf(properties);
  }

  public static Builder builder() {
    return new Builder();
  }

  public @Nullable String runtimeId() {
    return runtimeId;
  }

  public @Nullable String kubeContext() {
    return kubeContext;
  }

  public Map<String, String> properties() {
    return properties;
  }

  /** Internal: build the {@link HelmEngineConfig} a provider sees. */
  HelmEngineConfig toEngineConfig() {
    return new HelmEngineConfig(kubeContext, properties);
  }

  public static final class Builder {
    private @Nullable String runtimeId;
    private @Nullable String kubeContext;
    private final Map<String, String> properties = new LinkedHashMap<>();

    private Builder() {}

    public Builder runtime(String id) {
      this.runtimeId = id;
      return this;
    }

    public Builder kubeContext(String context) {
      this.kubeContext = context;
      return this;
    }

    public Builder property(String key, String value) {
      this.properties.put(
          Objects.requireNonNull(key, "key"), Objects.requireNonNull(value, "value"));
      return this;
    }

    public HelmClientOptions build() {
      return new HelmClientOptions(runtimeId, kubeContext, properties);
    }
  }
}
