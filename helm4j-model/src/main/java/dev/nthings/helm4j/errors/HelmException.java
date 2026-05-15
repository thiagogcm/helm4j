package dev.nthings.helm4j.errors;

import org.jspecify.annotations.Nullable;

/**
 * Base type for every Helm4j failure surfaced through the exception channel.
 *
 * <p>Three concrete subtypes split the failure space:
 *
 * <ul>
 *   <li>{@link HelmCommandException}: a Helm operation reached the runtime and the runtime reported
 *       it failed (a chart resolution error, a Kubernetes apply error, an authentication failure
 *       against an OCI registry).
 *   <li>{@link HelmRuntimeException}: the runtime itself failed before/while executing the
 *       operation — the native library was not found, the protocol response could not be parsed, an
 *       FFM allocation failed.
 *   <li>{@link HelmConfigurationException}: the request or client configuration was invalid before
 *       any runtime call (mandatory fields missing, mutually exclusive flags set).
 * </ul>
 *
 * <p>{@link HelmException} is unchecked so it does not pollute method signatures, but callers
 * handle it the same as any checked exception.
 */
public abstract class HelmException extends RuntimeException {

  protected HelmException(String message) {
    super(message);
  }

  protected HelmException(String message, @Nullable Throwable cause) {
    super(message, cause);
  }
}
