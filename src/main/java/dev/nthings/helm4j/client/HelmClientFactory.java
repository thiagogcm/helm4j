package dev.nthings.helm4j.client;

import java.util.Objects;

import dev.nthings.helm4j.bindings.FfmNativeHelmBindings;
import dev.nthings.helm4j.bindings.NativeHelmBindings;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/** Factory for creating configured {@link HelmClient} instances. */
public final class HelmClientFactory {

  private final ObjectMapper mapper;
  private final NativeHelmBindings bindings;

  private HelmClientFactory(ObjectMapper mapper, NativeHelmBindings bindings) {
    this.mapper = Objects.requireNonNull(mapper, "mapper");
    this.bindings = Objects.requireNonNull(bindings, "bindings");
  }

  public static HelmClientFactory create() {
    return new HelmClientFactory(defaultMapper(), new FfmNativeHelmBindings());
  }

  /** Override the ObjectMapper used for JSON serialization/deserialization. */
  public HelmClientFactory withObjectMapper(ObjectMapper mapper) {
    return new HelmClientFactory(Objects.requireNonNull(mapper, "mapper"), bindings);
  }

  /** Override the native transport used by the client. */
  public HelmClientFactory withNativeBindings(NativeHelmBindings bindings) {
    return new HelmClientFactory(mapper, Objects.requireNonNull(bindings, "bindings"));
  }

  /** Build a new {@link HelmClient} using the configured options. */
  public HelmClient newClient() {
    return new HelmClient(mapper, bindings);
  }

  private static ObjectMapper defaultMapper() {
    return JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
  }
}
