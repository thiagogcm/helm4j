package dev.nthings.helm4j.release;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * Outcome of a successful uninstall. A {@code null} {@link #release()} indicates that {@code
 * ignoreNotFound} matched a release that did not exist.
 */
public record UninstallReport(@Nullable Release release, String info) {

  public UninstallReport {
    info = Objects.requireNonNullElse(info, "");
  }
}
