package dev.nthings.helm4j;

import java.util.Objects;
import java.util.function.Consumer;

/** Standard static SDK entrypoint for Helm operations. */
public final class Helm {

  private Helm() {}

  public static HelmClient client() {
    return HelmClient.builder().build();
  }

  public static HelmClient client(Consumer<HelmClient.Builder> spec) {
    Objects.requireNonNull(spec, "spec");
    var builder = HelmClient.builder();
    spec.accept(builder);
    return builder.build();
  }
}
