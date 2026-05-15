package dev.nthings.helm4j.runtime.ffm;

import dev.nthings.helm4j.runtime.ffm.internal.FfmHelmBridge;
import dev.nthings.helm4j.runtime.ffm.internal.NativeHelmEngine;
import dev.nthings.helm4j.spi.HelmEngine;
import dev.nthings.helm4j.spi.HelmEngineConfig;
import dev.nthings.helm4j.spi.HelmEngineProvider;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/** Default {@link HelmEngineProvider}: bridges to {@code libhelm4j.so} through the FFM runtime. */
public final class FfmHelmEngineProvider implements HelmEngineProvider {

  /** Stable identifier for this provider. Used by {@code HelmClientOptions.runtime("native")}. */
  public static final String ID = "native";

  @Override
  public String id() {
    return ID;
  }

  @Override
  public HelmEngine create(HelmEngineConfig config) {
    return new NativeHelmEngine(new FfmHelmBridge(), DefaultMapperHolder.INSTANCE, config);
  }

  /** Lazy holder for the default {@link ObjectMapper}. Stateless and shared. */
  private static final class DefaultMapperHolder {
    static final ObjectMapper INSTANCE =
        JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
  }
}
