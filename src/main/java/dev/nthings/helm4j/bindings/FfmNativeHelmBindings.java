package dev.nthings.helm4j.bindings;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import dev.nthings.helm4j.model.ShowMode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** FFM-backed implementation of {@link NativeHelmBindings}. */
public final class FfmNativeHelmBindings implements NativeHelmBindings {

  private static final Logger LOGGER = LoggerFactory.getLogger(FfmNativeHelmBindings.class);

  private static final String LIBRARY_PATH = "libhelm4j/libhelm4j.so";
  private static final String FREE_STRING_SYMBOL = "FreeString";
  private static final String HELM_SEARCH_SYMBOL = "HelmSearch";
  private static final String HELM_SHOW_CHART_SYMBOL = "HelmShowChart";
  private static final String HELM_SHOW_VALUES_SYMBOL = "HelmShowValues";
  private static final String HELM_SHOW_README_SYMBOL = "HelmShowReadme";
  private static final String HELM_SHOW_ALL_SYMBOL = "HelmShowAll";
  private static final String HELM_SHOW_CRDS_SYMBOL = "HelmShowCRDs";

  private static final FunctionDescriptor SHOW_FUNCTION_DESCRIPTOR =
      FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS);
  private static final FunctionDescriptor SEARCH_FUNCTION_DESCRIPTOR =
      FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS);
  private static final FunctionDescriptor FREE_FUNCTION_DESCRIPTOR =
      FunctionDescriptor.ofVoid(ValueLayout.ADDRESS);

  private static final Arena LIBRARY_ARENA = Arena.ofAuto();
  private static final SymbolLookup SYMBOL_LOOKUP = createSymbolLookup();
  private static final ConcurrentHashMap<String, MethodHandle> HANDLE_CACHE =
      new ConcurrentHashMap<>();

  private final NativeShowInvoker showInvoker;
  private final NativeSearchInvoker searchInvoker;
  private final NativeStringReleaser stringReleaser;

  public FfmNativeHelmBindings() {
    this(
        FfmNativeHelmBindings::invokeShowNative,
        FfmNativeHelmBindings::invokeSearchNative,
        FfmNativeHelmBindings::freeNativeString);
  }

  FfmNativeHelmBindings(
      NativeShowInvoker showInvoker,
      NativeSearchInvoker searchInvoker,
      NativeStringReleaser stringReleaser) {
    this.showInvoker = Objects.requireNonNull(showInvoker, "showInvoker");
    this.searchInvoker = Objects.requireNonNull(searchInvoker, "searchInvoker");
    this.stringReleaser = Objects.requireNonNull(stringReleaser, "stringReleaser");
  }

  @Override
  public String show(ShowMode mode, String chartReference, String optionsJson) {
    Objects.requireNonNull(mode, "mode");
    Objects.requireNonNull(chartReference, "chartReference");
    Objects.requireNonNull(optionsJson, "optionsJson");

    try (var arena = Arena.ofConfined()) {
      var chartRefPtr = arena.allocateFrom(chartReference);
      var optionsPtr = arena.allocateFrom(optionsJson);
      var resultPtr = showInvoker.invoke(mode, chartRefPtr, optionsPtr);
      return readAndFree(resultPtr);
    }
  }

  @Override
  public String search(String optionsJson) {
    Objects.requireNonNull(optionsJson, "optionsJson");

    try (var arena = Arena.ofConfined()) {
      var optionsPtr = arena.allocateFrom(optionsJson);
      var resultPtr = searchInvoker.invoke(optionsPtr);
      return readAndFree(resultPtr);
    }
  }

  private String readAndFree(MemorySegment pointer) {
    if (pointer == null || pointer.equals(MemorySegment.NULL)) {
      throw new IllegalStateException("Native layer returned a null response pointer");
    }

    try {
      return pointer.reinterpret(Long.MAX_VALUE).getString(0);
    } finally {
      stringReleaser.free(pointer);
    }
  }

  private static MemorySegment invokeShowNative(
      ShowMode mode, MemorySegment chartRef, MemorySegment options) {
    var symbol =
        switch (mode) {
          case CHART -> HELM_SHOW_CHART_SYMBOL;
          case VALUES -> HELM_SHOW_VALUES_SYMBOL;
          case README -> HELM_SHOW_README_SYMBOL;
          case ALL -> HELM_SHOW_ALL_SYMBOL;
          case CRDS -> HELM_SHOW_CRDS_SYMBOL;
        };

    return invokeShowBySymbol(symbol, chartRef, options);
  }

  private static MemorySegment invokeShowBySymbol(
      String symbol, MemorySegment chartRef, MemorySegment options) {
    try {
      var handle = methodHandle(symbol, SHOW_FUNCTION_DESCRIPTOR);
      return (MemorySegment) handle.invokeExact(chartRef, options);
    } catch (Error | RuntimeException ex) {
      throw ex;
    } catch (Throwable ex) {
      throw new IllegalStateException("Failed invoking native function: " + symbol, ex);
    }
  }

  private static MemorySegment invokeSearchNative(MemorySegment options) {
    try {
      var handle = methodHandle(HELM_SEARCH_SYMBOL, SEARCH_FUNCTION_DESCRIPTOR);
      return (MemorySegment) handle.invokeExact(options);
    } catch (Error | RuntimeException ex) {
      throw ex;
    } catch (Throwable ex) {
      throw new IllegalStateException("Failed invoking native function: " + HELM_SEARCH_SYMBOL, ex);
    }
  }

  private static void freeNativeString(MemorySegment pointer) {
    try {
      var handle = methodHandle(FREE_STRING_SYMBOL, FREE_FUNCTION_DESCRIPTOR);
      handle.invokeExact(pointer);
    } catch (Error | RuntimeException ex) {
      throw ex;
    } catch (Throwable ex) {
      throw new IllegalStateException("Failed invoking native function: " + FREE_STRING_SYMBOL, ex);
    }
  }

  private static MethodHandle methodHandle(String symbol, FunctionDescriptor descriptor) {
    return HANDLE_CACHE.computeIfAbsent(
        symbol,
        ignored -> {
          var address =
              SYMBOL_LOOKUP
                  .find(symbol)
                  .orElseThrow(
                      () ->
                          new IllegalStateException(
                              "Native symbol not found: "
                                  + symbol
                                  + ". Ensure libhelm4j is built and visible to the runtime."));
          return Linker.nativeLinker().downcallHandle(address, descriptor);
        });
  }

  private static SymbolLookup createSymbolLookup() {
    var fallback = SymbolLookup.loaderLookup().or(Linker.nativeLinker().defaultLookup());

    try {
      return SymbolLookup.libraryLookup(LIBRARY_PATH, LIBRARY_ARENA).or(fallback);
    } catch (IllegalArgumentException | UnsatisfiedLinkError ex) {
      LOGGER.debug(
          "Unable to eagerly load native library '{}'. Falling back to loader/default lookup.",
          LIBRARY_PATH,
          ex);
      return fallback;
    }
  }

  @FunctionalInterface
  interface NativeShowInvoker {
    MemorySegment invoke(ShowMode mode, MemorySegment chartRef, MemorySegment options);
  }

  @FunctionalInterface
  interface NativeSearchInvoker {
    MemorySegment invoke(MemorySegment options);
  }

  @FunctionalInterface
  interface NativeStringReleaser {
    void free(MemorySegment pointer);
  }
}
