# Helm4j

Helm4j is an experimental Java 25 API for Helm v4. It uses typed requests and results instead of invoking the Helm CLI. The default runtime calls Helm's Go SDK through a native `libhelm4j.so` bridge and Java's Foreign Function & Memory API.

## Current status

- Release, chart, repository, registry, and version operations are implemented end to end.
- Helm is pinned to `helm.sh/helm/v4 v4.2.2`.
- The native runtime currently targets Linux and is built from source.
- Artifacts are not published. The project version is `1.0-SNAPSHOT`.

See [feature parity](docs/feature-parity.md) for supported operations and known gaps.

## Build

Requirements:

- JDK 25
- Go 1.26.4 with cgo and a C toolchain
- `just` 1.40 or newer
- `ripgrep`

Build the native library and all Java modules:

```bash
just build
```

Run the full Go, native parity, Java, and coverage checks:

```bash
just check
```

Use `just --list` for focused tasks. Regenerating the checked-in FFM bindings also requires `jextract` and `LLVM_HOME`.

## Use the client

Applications compile against `helm4j-client` and load a runtime provider at runtime. In this repository, `helm4j-samples` uses project dependencies for that split:

```kotlin
implementation(project(":helm4j-client"))
runtimeOnly(project(":helm4j-runtime-native"))
```

A modular application requires only the client module:

```java
module my.application {
  requires dev.nthings.helm4j;
}
```

Create one client, use its domain namespaces, and close it with try-with-resources:

```java
import dev.nthings.helm4j.HelmClient;
import dev.nthings.helm4j.chart.ChartRef;

try (var helm = HelmClient.create()) {
  var release =
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

Each operation also accepts a pre-built request. Failures throw a `HelmException`; use `HelmResult.capture(...)` when a value-or-error result is more convenient.

## Run the samples

Build first so `libhelm4j.so` exists, then run the offline examples:

```bash
./gradlew :helm4j-samples:run --args=offline
```

Available sample IDs are `system`, `charts`, `repositories`, and `releases`. With no arguments, the runner executes all four; repository examples need network access and release examples need a Kubernetes cluster.

## Documentation

| Document                                 | Purpose                                                 |
| ---------------------------------------- | ------------------------------------------------------- |
| [Public API](docs/public-api.md)         | Client entry points, operations, results, and errors    |
| [Architecture](docs/architecture.md)     | Module boundaries, provider discovery, and native calls |
| [Feature parity](docs/feature-parity.md) | Implemented Helm operations and current limitations     |
