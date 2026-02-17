package dev.nthings.helm4j.bindings;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Objects;

import dev.nthings.helm4j.jextract.libhelm4j_h;
import dev.nthings.helm4j.model.ShowMode;

/** FFM-backed implementation of {@link NativeHelmBindings}. */
public final class FfmNativeHelmBindings implements NativeHelmBindings {

  private static final String LIBRARY_HINT =
      "Ensure libhelm4j is built and visible to the runtime (for jextract also set LLVM_HOME and"
          + " LD_LIBRARY_PATH when generating bindings).";

  private static final String FREE_STRING_SYMBOL = "FreeString";
  private static final String HELM_SEARCH_SYMBOL = "HelmSearch";
  private static final String HELM_SHOW_CHART_SYMBOL = "HelmShowChart";
  private static final String HELM_SHOW_VALUES_SYMBOL = "HelmShowValues";
  private static final String HELM_SHOW_README_SYMBOL = "HelmShowReadme";
  private static final String HELM_SHOW_ALL_SYMBOL = "HelmShowAll";
  private static final String HELM_SHOW_CRDS_SYMBOL = "HelmShowCRDs";
  private static final String HELM_REPO_ADD_SYMBOL = "HelmRepoAdd";
  private static final String HELM_REPO_UPDATE_SYMBOL = "HelmRepoUpdate";
  private static final String HELM_REPO_LIST_SYMBOL = "HelmRepoList";
  private static final String HELM_REPO_REMOVE_SYMBOL = "HelmRepoRemove";

  private final NativeShowInvoker showInvoker;
  private final NativeUnaryInvoker searchInvoker;
  private final NativeUnaryInvoker repoAddInvoker;
  private final NativeUnaryInvoker repoUpdateInvoker;
  private final NativeUnaryInvoker repoListInvoker;
  private final NativeUnaryInvoker repoRemoveInvoker;
  private final NativeStringReleaser stringReleaser;

  public FfmNativeHelmBindings() {
    this(
        FfmNativeHelmBindings::invokeShowNative,
        FfmNativeHelmBindings::invokeSearchNative,
        FfmNativeHelmBindings::invokeRepoAddNative,
        FfmNativeHelmBindings::invokeRepoUpdateNative,
        FfmNativeHelmBindings::invokeRepoListNative,
        FfmNativeHelmBindings::invokeRepoRemoveNative,
        FfmNativeHelmBindings::freeNativeString);
  }

  FfmNativeHelmBindings(
      NativeShowInvoker showInvoker,
      NativeUnaryInvoker searchInvoker,
      NativeUnaryInvoker repoAddInvoker,
      NativeUnaryInvoker repoUpdateInvoker,
      NativeUnaryInvoker repoListInvoker,
      NativeUnaryInvoker repoRemoveInvoker,
      NativeStringReleaser stringReleaser) {
    this.showInvoker = Objects.requireNonNull(showInvoker, "showInvoker");
    this.searchInvoker = Objects.requireNonNull(searchInvoker, "searchInvoker");
    this.repoAddInvoker = Objects.requireNonNull(repoAddInvoker, "repoAddInvoker");
    this.repoUpdateInvoker = Objects.requireNonNull(repoUpdateInvoker, "repoUpdateInvoker");
    this.repoListInvoker = Objects.requireNonNull(repoListInvoker, "repoListInvoker");
    this.repoRemoveInvoker = Objects.requireNonNull(repoRemoveInvoker, "repoRemoveInvoker");
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
    return invokeUnary(optionsJson, searchInvoker);
  }

  @Override
  public String repoAdd(String optionsJson) {
    return invokeUnary(optionsJson, repoAddInvoker);
  }

  @Override
  public String repoUpdate(String optionsJson) {
    return invokeUnary(optionsJson, repoUpdateInvoker);
  }

  @Override
  public String repoList(String optionsJson) {
    return invokeUnary(optionsJson, repoListInvoker);
  }

  @Override
  public String repoRemove(String optionsJson) {
    return invokeUnary(optionsJson, repoRemoveInvoker);
  }

  private String invokeUnary(String optionsJson, NativeUnaryInvoker invoker) {
    Objects.requireNonNull(optionsJson, "optionsJson");

    try (var arena = Arena.ofConfined()) {
      var optionsPtr = arena.allocateFrom(optionsJson);
      var resultPtr = invoker.invoke(optionsPtr);
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
    return switch (mode) {
      case CHART ->
          invokeJextract(
              HELM_SHOW_CHART_SYMBOL, () -> libhelm4j_h.HelmShowChart(chartRef, options));
      case VALUES ->
          invokeJextract(
              HELM_SHOW_VALUES_SYMBOL, () -> libhelm4j_h.HelmShowValues(chartRef, options));
      case README ->
          invokeJextract(
              HELM_SHOW_README_SYMBOL, () -> libhelm4j_h.HelmShowReadme(chartRef, options));
      case ALL ->
          invokeJextract(HELM_SHOW_ALL_SYMBOL, () -> libhelm4j_h.HelmShowAll(chartRef, options));
      case CRDS ->
          invokeJextract(HELM_SHOW_CRDS_SYMBOL, () -> libhelm4j_h.HelmShowCRDs(chartRef, options));
    };
  }

  private static MemorySegment invokeSearchNative(MemorySegment options) {
    return invokeJextract(HELM_SEARCH_SYMBOL, () -> libhelm4j_h.HelmSearch(options));
  }

  private static MemorySegment invokeRepoAddNative(MemorySegment options) {
    return invokeJextract(HELM_REPO_ADD_SYMBOL, () -> libhelm4j_h.HelmRepoAdd(options));
  }

  private static MemorySegment invokeRepoUpdateNative(MemorySegment options) {
    return invokeJextract(HELM_REPO_UPDATE_SYMBOL, () -> libhelm4j_h.HelmRepoUpdate(options));
  }

  private static MemorySegment invokeRepoListNative(MemorySegment options) {
    return invokeJextract(HELM_REPO_LIST_SYMBOL, () -> libhelm4j_h.HelmRepoList(options));
  }

  private static MemorySegment invokeRepoRemoveNative(MemorySegment options) {
    return invokeJextract(HELM_REPO_REMOVE_SYMBOL, () -> libhelm4j_h.HelmRepoRemove(options));
  }

  private static void freeNativeString(MemorySegment pointer) {
    invokeJextractVoid(FREE_STRING_SYMBOL, () -> libhelm4j_h.FreeString(pointer));
  }

  private static MemorySegment invokeJextract(String symbol, NativeInvocation invocation) {
    try {
      return invocation.invoke();
    } catch (Throwable ex) {
      throw new IllegalStateException(
          "Failed invoking native function: " + symbol + ". " + LIBRARY_HINT, ex);
    }
  }

  private static void invokeJextractVoid(String symbol, NativeVoidInvocation invocation) {
    try {
      invocation.invoke();
    } catch (Throwable ex) {
      throw new IllegalStateException(
          "Failed invoking native function: " + symbol + ". " + LIBRARY_HINT, ex);
    }
  }

  @FunctionalInterface
  interface NativeShowInvoker {
    MemorySegment invoke(ShowMode mode, MemorySegment chartRef, MemorySegment options);
  }

  @FunctionalInterface
  interface NativeUnaryInvoker {
    MemorySegment invoke(MemorySegment options);
  }

  @FunctionalInterface
  interface NativeStringReleaser {
    void free(MemorySegment pointer);
  }

  @FunctionalInterface
  private interface NativeInvocation {
    MemorySegment invoke() throws Throwable;
  }

  @FunctionalInterface
  private interface NativeVoidInvocation {
    void invoke() throws Throwable;
  }
}
