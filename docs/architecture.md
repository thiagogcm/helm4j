# Helm4j Architecture

Helm4j is a Java 25 library that drives Helm by calling a Go-built native shared library (`libhelm4j`) over the Foreign Function & Memory (FFM) API. It is split into JPMS modules along audience lines so the public, user-facing API carries none of the native plumbing.

## Modules

```
helm4j-samples   module dev.nthings.helm4j.samples   Runnable end-to-end examples; depends
                                                      only on the client, same as any app.
      │ requires
      ▼
helm4j-client    module dev.nthings.helm4j           The consumer entry point: HelmClient,
                                                      HelmClientOptions, the five namespace
                                                      clients, HelmResult. Discovers a
                                                      runtime via ServiceLoader.
      │ requires transitive
      ├──────────────► helm4j-model  module dev.nthings.helm4j.model
      │ requires                     Pure SDK vocabulary: request/response records, value
      ▼                              types (ChartRef, ChartSource, Credentials, TlsOptions),
helm4j-spi       module dev.nthings.helm4j.spi        enums, the HelmException family,
                 The provider contract: HelmEngine,    ListResult. requires org.jspecify only.
                 HelmEngineProvider, HelmEngineConfig,
                 and the five domain gateways.
                 requires transitive helm4j-model.
      ▲ provides HelmEngineProvider
      │
helm4j-runtime-native  module dev.nthings.helm4j.runtime.ffm
                 The FFM runtime: jextract bindings, the JSON bridge, option/payload
                 mapping, native library lookup, and the FfmHelmEngineProvider supplied
                 via ServiceLoader. One provider among potentially many.
                 requires helm4j-spi (+ helm4j-model transitively), Jackson, SLF4J.
      │ FFM / cgo
      ▼
libhelm4j        Go/cgo shared library (libhelm4j.so). Thin //export functions over
                 testable Go operation packages. An implementation detail of the runtime.
```

The dependency direction is acyclic. `helm4j-client` and `helm4j-runtime-native` both depend on `helm4j-spi`; nothing depends on the client; no package name appears in more than one module.

## The seam

`dev.nthings.helm4j.spi` is the boundary between the SDK and any runtime. A `HelmEngineProvider` is discovered through `java.util.ServiceLoader` (`HelmClient.create()`); `helm4j-runtime-native` declares the FFM-backed implementation via `provides ... with` in its `module-info`. An alternative runtime — a process/CLI-based engine, an in-memory fake, a remote engine — only needs to implement the SPI and be discoverable; it does not depend on the native module. `HelmClient.using(HelmEngine)` wraps such an implementation directly and is the advanced/test entry point.

A `HelmEngine` fans out into five domain gateways — `releases()`, `charts()`, `repositories()`, `registries()`, `system()` — each accepting model requests and returning model responses. The namespace clients on `HelmClient` are thin wrappers over these gateways; each operation offers a `Consumer<Builder>` entry point and a pre-built-request overload.

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
}
```

## Failure handling

Namespace methods throw by default. Every failure is a `HelmException`:

- `HelmCommandException` — the operation reached the runtime and Helm reported it failed; carries a structured `HelmFailure` (message, stage, operation).
- `HelmRuntimeException` — the runtime itself failed (native library not found, FFM allocation, protocol parse error).
- `HelmConfigurationException` — invalid request/client configuration caught before any runtime call.

For no-throw workflows, wrap a call in `HelmResult.capture(() -> ...)` and inspect the result. The pending-on-cluster case is encoded by `Release.status()`, not a separate result type.

## Marshalling

`helm4j-runtime-native` maps each SDK request to a `Map<String, Object>` of wire options (`NativeOptions`), serializes it to JSON (Jackson), and crosses the FFM boundary as UTF-8 byte arrays via the `HelmBridge` transport. The Go logic in `libhelm4j` executes the operation and returns a JSON envelope, which is deserialized into runtime-private payload records and then mapped by hand into the `helm4j-model` result records. Only the native module's internal payload package is `open` to Jackson; `helm4j-model` stays pure data with no Jackson coupling.

## Depending on Helm4j

Applications compile against `helm4j-client` (which brings `helm4j-model` transitively) and put `helm4j-runtime-native` on the runtime path:

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
