package dev.nthings.helm4j.client;

import java.util.Objects;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/** Factory for creating configured {@link HelmClient} instances. */
public final class HelmClientFactory {

  private ObjectMapper mapper = defaultMapper();

  private HelmClientFactory() {}

  public static HelmClientFactory create() {
    return new HelmClientFactory();
  }

  /** Override the ObjectMapper used for JSON serialization/deserialization. */
  public HelmClientFactory withObjectMapper(ObjectMapper mapper) {
    this.mapper = Objects.requireNonNull(mapper, "mapper");
    return this;
  }

  /** Build a new {@link HelmClient} using the configured options. */
  public HelmClient newClient() {
    return new HelmClient(mapper);
  }

  private static ObjectMapper defaultMapper() {
    return JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
  }
}
