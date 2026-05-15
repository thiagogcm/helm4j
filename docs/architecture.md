# Helm4j Architecture

Helm4j is a Java 25 library that drives Helm by calling a Go-built native shared
library (`libhelm4j`) over the Foreign Function & Memory (FFM) API. It is split into
three strictly-layered JPMS modules so that the public, user-facing API carries none
of the native plumbing.

## Modules

```
helm4j-api      module dev.nthings.helm4j          Pure SDK vocabulary: request and
                                                   result records, sealed result
                                                   hierarchies, ChartRef, enums, the
                                                   HelmException family, ListResult.
                                                   requires: org.jspecify only.
      ▲ requires transitive
helm4j-spi      module dev.nthings.helm4j.spi       The gateway SPI plus the client
                                                   facade.
                                                     dev.nthings.helm4j.spi      —
                                                       HelmGatewayProvider,
                                                       HelmGateway and the four
                                                       sub-gateway interfaces.
                                                     dev.nthings.helm4j.client   —
                                                       Helm, HelmClient and the
                                                       namespace clients under
                                                       client.{repo,chart,release}.
                                                   requires transitive: dev.nthings.helm4j.
      ▲ requires
helm4j-native   module dev.nthings.helm4j.runtime   The FFM runtime: jextract bindings,
                                                   the JSON bridge, and the
                                                   FfmHelmGatewayProvider supplied via
                                                   ServiceLoader. One provider among
                                                   potentially many.
                                                   requires: dev.nthings.helm4j.spi.
```

The dependency direction is strictly linear and acyclic
(`native → spi → api`), and no package name appears in more than one module.

## The seam

`dev.nthings.helm4j.spi` is the boundary between the SDK and any runtime. A
`HelmGatewayProvider` is discovered through `java.util.ServiceLoader`
(`HelmClient.create()`); `helm4j-native` declares the FFM-backed implementation via
`provides ... with` in its `module-info`. An alternative runtime — a process/CLI-based
gateway, an in-memory fake, a test double — only needs to implement the SPI and be
discoverable; it does not depend on the native module. `HelmClient.using(HelmGateway)`
wraps such an implementation directly.

Requests are pure data. A request record's `Builder` only builds; it captures no
gateway and does not execute. Execution lives entirely in the namespace clients, each
operation offering a `Consumer<Builder>` entry point and a pre-built-request overload:

```java
try (var helm = Helm.client()) {
  helm.release().install(b -> b
      .releaseName("nginx")
      .chart(ChartRef.repo("bitnami/nginx"))
      .namespace("apps"));
}
```

## Marshalling

Domain requests are serialized to JSON (Jackson) in `helm4j-native`, crossed over the
FFM boundary as UTF-8 byte arrays / C strings via the `HelmBridge` transport, executed
by the Go logic in `libhelm4j`, and the JSON response is deserialized back into the
`helm4j-api` result records. The `helm4j-api` DTO packages are `open` to
`tools.jackson.databind` for this reason; `helm4j-api` itself does not depend on
Jackson.

## Depending on Helm4j

Consumers compile against `helm4j-spi` (which brings `helm4j-api` transitively) and put
`helm4j-native` on the runtime path:

```kotlin
dependencies {
    implementation("dev.nthings.helm4j:helm4j-spi")
    runtimeOnly("dev.nthings.helm4j:helm4j-native")
}
```
