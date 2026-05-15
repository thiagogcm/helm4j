package dev.nthings.helm4j.runtime.ffm.internal;

import java.nio.file.Files;
import java.nio.file.Path;

import dev.nthings.helm4j.runtime.ffm.FfmHelmEngineProvider;
import dev.nthings.helm4j.spi.HelmEngineConfig;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Smoke test that loads the real libhelm4j library and exercises the FFM bridge end to end. Skipped
 * when the native library has not been built (e.g. CI without the Go toolchain).
 */
@DisplayName("Native runtime smoke test (requires libhelm4j.so)")
class NativeRuntimeSmokeTest {

  @Test
  void providerLoadsNativeLibraryAndReportsVersion() {
    assumeTrue(
        Files.isRegularFile(Path.of("libhelm4j", "libhelm4j.so")),
        "libhelm4j.so not built; run `just go-build`");

    try (var engine = new FfmHelmEngineProvider().create(HelmEngineConfig.empty())) {
      var version = engine.system().version();
      assertNotNull(version);
      assertNotNull(version.version());
      assertNotNull(version.helmVersion());
    }
  }
}
