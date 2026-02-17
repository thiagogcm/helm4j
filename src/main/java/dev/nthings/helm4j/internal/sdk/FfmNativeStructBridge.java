package dev.nthings.helm4j.internal.sdk;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import dev.nthings.helm4j.jextract.libhelm4j_h;

/** FFM-backed implementation of JSON native bridge operations. */
public final class FfmNativeStructBridge implements NativeStructBridge {

  @Override
  public String repo(String mode, String optionsJson) {
    return invoke(
        "repo",
        arena ->
            libhelm4j_h.HelmRepo(cstringOrNull(arena, mode), cstringOrNull(arena, optionsJson)));
  }

  @Override
  public String search(String mode, String optionsJson) {
    return invoke(
        "search",
        arena ->
            libhelm4j_h.HelmSearch(cstringOrNull(arena, mode), cstringOrNull(arena, optionsJson)));
  }

  @Override
  public String show(String mode, String chartRef, String optionsJson) {
    return invoke(
        "show",
        arena ->
            libhelm4j_h.HelmShow(
                cstringOrNull(arena, mode),
                cstringOrNull(arena, chartRef),
                cstringOrNull(arena, optionsJson)));
  }

  @Override
  public String install(String releaseName, String chartRef, String optionsJson) {
    return invoke(
        "install",
        arena ->
            libhelm4j_h.HelmInstall(
                cstringOrNull(arena, releaseName),
                cstringOrNull(arena, chartRef),
                cstringOrNull(arena, optionsJson)));
  }

  private static String invoke(String operation, NativeCall call) {
    try (var arena = Arena.ofConfined()) {
      MemorySegment response;
      try {
        response = call.invoke(arena);
      } catch (RuntimeException error) {
        throw new IllegalStateException("Native invocation failed for " + operation, error);
      }

      if (response == null || response.equals(MemorySegment.NULL)) {
        return "";
      }

      try {
        return response.reinterpret(Long.MAX_VALUE).getString(0);
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

  private static MemorySegment cstringOrNull(Arena arena, String value) {
    if (value == null) {
      return MemorySegment.NULL;
    }
    return arena.allocateFrom(value);
  }

  @FunctionalInterface
  private interface NativeCall {
    MemorySegment invoke(Arena arena);
  }
}
