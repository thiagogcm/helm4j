package dev.nthings.helm4j.runtime.ffm.internal;

import java.io.File;
import java.lang.foreign.Arena;
import java.lang.foreign.SymbolLookup;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Resolves the {@code libhelm4j} native library for the jextract-generated bindings.
 *
 * <p>jextract emits a fixed, working-directory-relative path. This helper replaces that single
 * lookup with a resilient search so the bindings work both from the development tree and from a
 * packaged consumer application:
 *
 * <ol>
 *   <li>{@code -Dhelm4j.library.path=<path>} — explicit override (file or directory).
 *   <li>{@code libhelm4j/libhelm4j.so} relative to the working directory — the development layout.
 *   <li>each entry of {@code java.library.path}.
 *   <li>a bare-name lookup honouring the OS loader search path ({@code LD_LIBRARY_PATH}, etc.).
 * </ol>
 *
 * <p>If none resolve, an empty {@link SymbolLookup} is returned so the caller's {@code
 * loaderLookup}/default-lookup fallbacks still apply (e.g. a host that pre-loaded the library).
 */
public final class NativeLibrary {

  private static final String LIBRARY_FILE_NAME = System.mapLibraryName("helm4j");

  private NativeLibrary() {}

  /** Resolves a {@link SymbolLookup} for {@code libhelm4j}; never throws "not found". */
  public static SymbolLookup symbolLookup(Arena arena) {
    for (Path candidate : candidatePaths()) {
      if (Files.isRegularFile(candidate)) {
        return SymbolLookup.libraryLookup(candidate, arena);
      }
    }
    try {
      return SymbolLookup.libraryLookup(LIBRARY_FILE_NAME, arena);
    } catch (IllegalArgumentException notOnLoaderPath) {
      return name -> Optional.empty();
    }
  }

  private static List<Path> candidatePaths() {
    var paths = new ArrayList<Path>();

    String override = System.getProperty("helm4j.library.path");
    if (override != null && !override.isBlank()) {
      Path overridePath = Path.of(override.trim());
      paths.add(
          Files.isDirectory(overridePath) ? overridePath.resolve(LIBRARY_FILE_NAME) : overridePath);
    }

    paths.add(Path.of("libhelm4j", LIBRARY_FILE_NAME));

    String libraryPath = System.getProperty("java.library.path", "");
    for (String entry : libraryPath.split(File.pathSeparator)) {
      if (!entry.isBlank()) {
        paths.add(Path.of(entry.trim(), LIBRARY_FILE_NAME));
      }
    }

    return paths;
  }
}
