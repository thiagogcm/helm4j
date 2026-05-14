package dev.nthings.helm4j.internal.api;

import org.jspecify.annotations.Nullable;

/** Shared support for fluent request builders that can execute themselves against a gateway. */
public final class Invocations {

  private Invocations() {}

  /**
   * Returns {@code gateway} if the builder is bound to a client, otherwise fails with a message
   * pointing the caller at the runnable entry points.
   */
  public static <G> G requireBound(@Nullable G gateway) {
    if (gateway == null) {
      throw new IllegalStateException(
          "This builder was created via Request.builder() and is not bound to a Helm client. "
              + "Obtain a runnable builder from a namespace client (e.g. helm.release().install()),"
              + " or build() the request and pass it to the matching client method.");
    }
    return gateway;
  }
}
