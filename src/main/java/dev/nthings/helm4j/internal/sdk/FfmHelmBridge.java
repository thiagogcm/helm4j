package dev.nthings.helm4j.internal.sdk;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import dev.nthings.helm4j.jextract.libhelm4j_h;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FFM-backed implementation of the {@link HelmBridge} SPI using
 * jextract-generated bindings.
 */
public final class FfmHelmBridge implements HelmBridge {

  private static final Logger log = LoggerFactory.getLogger(FfmHelmBridge.class);

  @Override
  public byte[] repo(byte[] mode, byte[] optionsJson) {
    return invoke(
        "repo",
        arena -> libhelm4j_h.HelmRepo(
            cstringFromBytes(arena, mode), cstringFromBytes(arena, optionsJson)));
  }

  @Override
  public byte[] search(byte[] mode, byte[] optionsJson) {
    return invoke(
        "search",
        arena -> libhelm4j_h.HelmSearch(
            cstringFromBytes(arena, mode), cstringFromBytes(arena, optionsJson)));
  }

  @Override
  public byte[] show(byte[] mode, byte[] chartRef, byte[] optionsJson) {
    return invoke(
        "show",
        arena -> libhelm4j_h.HelmShow(
            cstringFromBytes(arena, mode),
            cstringFromBytes(arena, chartRef),
            cstringFromBytes(arena, optionsJson)));
  }

  @Override
  public byte[] install(byte[] releaseName, byte[] chartRef, byte[] optionsJson) {
    return invoke(
        "install",
        arena -> libhelm4j_h.HelmInstall(
            cstringFromBytes(arena, releaseName),
            cstringFromBytes(arena, chartRef),
            cstringFromBytes(arena, optionsJson)));
  }

  @Override
  public byte[] upgrade(byte[] releaseName, byte[] chartRef, byte[] optionsJson) {
    return invoke(
        "upgrade",
        arena -> libhelm4j_h.HelmUpgrade(
            cstringFromBytes(arena, releaseName),
            cstringFromBytes(arena, chartRef),
            cstringFromBytes(arena, optionsJson)));
  }

  @Override
  public byte[] uninstall(byte[] releaseName, byte[] optionsJson) {
    return invoke(
        "uninstall",
        arena -> libhelm4j_h.HelmUninstall(
            cstringFromBytes(arena, releaseName), cstringFromBytes(arena, optionsJson)));
  }

  @Override
  public byte[] status(byte[] releaseName, byte[] optionsJson) {
    return invoke(
        "status",
        arena -> libhelm4j_h.HelmStatus(
            cstringFromBytes(arena, releaseName), cstringFromBytes(arena, optionsJson)));
  }

  @Override
  public byte[] rollback(byte[] releaseName, byte[] optionsJson) {
    return invoke(
        "rollback",
        arena -> libhelm4j_h.HelmRollback(
            cstringFromBytes(arena, releaseName), cstringFromBytes(arena, optionsJson)));
  }

  @Override
  public byte[] history(byte[] releaseName, byte[] optionsJson) {
    return invoke(
        "history",
        arena -> libhelm4j_h.HelmHistory(
            cstringFromBytes(arena, releaseName), cstringFromBytes(arena, optionsJson)));
  }

  @Override
  public byte[] get(byte[] mode, byte[] releaseName, byte[] optionsJson) {
    return invoke(
        "get",
        arena -> libhelm4j_h.HelmGet(
            cstringFromBytes(arena, mode),
            cstringFromBytes(arena, releaseName),
            cstringFromBytes(arena, optionsJson)));
  }

  @Override
  public byte[] template(byte[] releaseName, byte[] chartRef, byte[] optionsJson) {
    return invoke(
        "template",
        arena -> libhelm4j_h.HelmTemplate(
            cstringFromBytes(arena, releaseName),
            cstringFromBytes(arena, chartRef),
            cstringFromBytes(arena, optionsJson)));
  }

  @Override
  public byte[] lint(byte[] chartPath, byte[] optionsJson) {
    return invoke(
        "lint",
        arena -> libhelm4j_h.HelmLint(
            cstringFromBytes(arena, chartPath), cstringFromBytes(arena, optionsJson)));
  }

  @Override
  public byte[] version() {
    return invoke("version", arena -> libhelm4j_h.HelmVersion());
  }

  private static byte[] invoke(String operation, NativeCall call) {
    log.debug("Invoking native operation: {}", operation);
    try (var arena = Arena.ofConfined()) {
      MemorySegment response;
      try {
        response = call.invoke(arena);
      } catch (RuntimeException error) {
        throw new IllegalStateException("Native invocation failed for " + operation, error);
      }

      if (response == null || response.equals(MemorySegment.NULL)) {
        log.debug("Native operation {} returned null/empty response", operation);
        return new byte[0];
      }

      try {
        var reinterpreted = response.reinterpret(Long.MAX_VALUE);
        long length = 0;
        while (reinterpreted.get(ValueLayout.JAVA_BYTE, length) != 0) {
          length++;
        }
        var result = reinterpreted.asSlice(0, length).toArray(ValueLayout.JAVA_BYTE);
        log.debug("Native operation {} returned {} bytes", operation, result.length);
        return result;
      } finally {
        free(response, operation);
      }
    }
  }

  private static void free(MemorySegment value, String operation) {
    try {
      libhelm4j_h.FreeString(value);
    } catch (RuntimeException error) {
      throw new IllegalStateException("Native free failed for " + operation, error);
    }
  }

  private static MemorySegment cstringFromBytes(Arena arena, byte[] bytes) {
    if (bytes == null || bytes.length == 0) {
      return MemorySegment.NULL;
    }
    var segment = arena.allocate(bytes.length + 1L);
    MemorySegment.copy(bytes, 0, segment, ValueLayout.JAVA_BYTE, 0, bytes.length);
    segment.set(ValueLayout.JAVA_BYTE, bytes.length, (byte) 0);
    return segment;
  }

  @FunctionalInterface
  private interface NativeCall {
    MemorySegment invoke(Arena arena);
  }
}
