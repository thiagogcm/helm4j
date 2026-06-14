# Helm4j

Helm4j is a Java SDK for Helm v4 focused on idiomatic Java APIs and a stable native bridge.

## Requirements

- Java 25+
- Go 1.26+
- Helm v4 SDK (bundled via `libhelm4j/go.mod`)

## Build

Development tasks are exposed through the root `Justfile`.

```bash
just --list
```

### Build Native Library

```bash
just go-build
```

### Build Java SDK

```bash
just build
```

### Run Checks

```bash
just check
```

## Highlights

- Idiomatic Java SDK for Helm v4 — typed requests, immutable record results, no CLI shelling.
- Modules split by audience: `helm4j-client` for apps, `helm4j-spi` for runtime providers, `helm4j-model` for shared vocabulary, `helm4j-runtime-native` for the FFM/`libhelm4j` runtime.
- Runtime discovered via `ServiceLoader`; swap in a process-based, remote, or in-memory engine without touching application code.
- Throws by default (`HelmException` family); opt into no-throw capture with `HelmResult.capture`.

## Quick Start

Compile against the client; put a runtime on the runtime path:

```kotlin
dependencies {
    implementation("dev.nthings.helm4j:helm4j-client")
    runtimeOnly("dev.nthings.helm4j:helm4j-runtime-native")
}
```

```java
module my.application {
  requires dev.nthings.helm4j;
}
```

```java
try (var helm = HelmClient.create()) {
  Release release =
      helm.releases()
          .install(
              b ->
                  b.releaseName("nginx")
                      .chart(ChartRef.repo("bitnami/nginx"))
                      .namespace("apps")
                      .createNamespace(true));

  System.out.println(release.name() + " is " + release.status());
}
```

The five namespaces are `helm.releases()`, `helm.charts()`, `helm.repositories()`, `helm.registries()`, and `helm.system()`. See `helm4j-samples` for an end-to-end walk-through and `docs/architecture.md` for the module design.
