package dev.nthings.helm4j.bindings;

import dev.nthings.helm4j.model.ShowMode;

/** Internal transport abstraction over native Helm bindings. */
public interface NativeHelmBindings {

  /** Runs a {@code helm show <mode>} operation and returns the native JSON payload. */
  String show(ShowMode mode, String chartReference, String optionsJson);

  /** Runs a {@code helm search repo} operation and returns the native JSON payload. */
  String search(String optionsJson);

  /** Runs a {@code helm repo add} operation and returns the native JSON payload. */
  String repoAdd(String optionsJson);

  /** Runs a {@code helm repo update} operation and returns the native JSON payload. */
  String repoUpdate(String optionsJson);

  /** Runs a {@code helm repo list} operation and returns the native JSON payload. */
  String repoList(String optionsJson);

  /** Runs a {@code helm repo remove} operation and returns the native JSON payload. */
  String repoRemove(String optionsJson);
}
