package dev.nthings.helm4j.internal.runtime;

import dev.nthings.helm4j.internal.spi.HelmGateway;
import dev.nthings.helm4j.internal.spi.HelmGatewayProvider;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/** Default {@link HelmGatewayProvider} bridging to libhelm4j through the FFM runtime. */
public final class FfmHelmGatewayProvider implements HelmGatewayProvider {

  @Override
  public HelmGateway create() {
    return new NativeStructGateway(new FfmHelmBridge(), DefaultMapperHolder.INSTANCE);
  }

  /**
   * Lazy holder for the default {@link ObjectMapper}. The mapper is stateless w.r.t. configuration
   * and only used for the internal JSON bridge protocol, so a single shared instance is safe.
   */
  private static final class DefaultMapperHolder {
    static final ObjectMapper INSTANCE =
        JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
  }
}
