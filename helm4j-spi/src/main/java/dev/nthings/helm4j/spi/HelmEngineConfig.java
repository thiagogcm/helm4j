package dev.nthings.helm4j.spi;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

/**
 * Configuration handed to {@link HelmEngineProvider#create(HelmEngineConfig)}.
 *
 * <p>Providers should honor whichever fields they understand and ignore the rest — extra entries in
 * {@link #properties()} are not an error. {@link #kubeContext()} is surfaced as a first-class field
 * because every backend understands it; everything else flows through the properties map until it
 * earns a typed accessor.
 */
public record HelmEngineConfig(@Nullable String kubeContext, Map<String, String> properties) {

  public HelmEngineConfig {
    properties = Map.copyOf(Objects.requireNonNull(properties, "properties"));
  }

  public static HelmEngineConfig empty() {
    return new HelmEngineConfig(null, Map.of());
  }

  public Optional<String> property(String key) {
    return Optional.ofNullable(properties.get(Objects.requireNonNull(key, "key")));
  }
}
