package dev.nthings.helm4j.internal.sdk;

/** Thin native bridge interface over JSON operations exposed by libhelm4j. */
public interface NativeStructBridge {

  String repo(String mode, String optionsJson);

  String search(String mode, String optionsJson);

  String show(String mode, String chartRef, String optionsJson);

  String install(String releaseName, String chartRef, String optionsJson);
}
