# Helm4j New Project Design

This document defines the target design for Helm4j as a greenfield SDK. It is not constrained by source or binary compatibility with the current codebase. The goal is a clean, modern Java API for future consumers, with module boundaries that make each audience and responsibility explicit.

## Consensus Summary

The new design keeps the useful shape of the current project, but renames and splits modules around audience:

- Consumers depend on `helm4j-client` and use JPMS module `dev.nthings.helm4j`.
- Public Helm vocabulary moves to `helm4j-model`.
- Provider contracts stay in a slim `helm4j-spi`.
- The FFM/libhelm4j implementation becomes `helm4j-runtime-native`.
- Test doubles and request capture live in a separate `helm4j-testing` module after the SPI stabilizes.

The biggest design change is that `helm4j-spi` must stop containing the consumer client. SPI is for runtime implementors. The client is for application developers.

## Current-State Diagnosis

Current modules:

| Current artifact | Current JPMS module | Current responsibility | Boundary issue |
| --- | --- | --- | --- |
| `helm4j-api` | `dev.nthings.helm4j` | Request records, result records, chart refs, release/repo models, errors, common list wrapper | This is really a model module, not the full API. Consumers cannot execute Helm with it alone. |
| `helm4j-spi` | `dev.nthings.helm4j.spi` | Gateway SPI and the user-facing `Helm`/`HelmClient` facade | Two audiences are mixed: consumers and runtime providers. Consumers must depend on an artifact named `spi`. |
| `helm4j-native` | `dev.nthings.helm4j.runtime` | ServiceLoader provider, native FFM bridge, jextract bindings, JSON protocol mapping, native library discovery | Correctly hidden from normal consumers, but internally mixes transport, protocol, mapping, and provider wiring. |
| `helm4j-samples` | `dev.nthings.helm4j.samples` | Runnable end-to-end examples | Should consume the same client artifact as applications. |
| `libhelm4j` | Go/cgo native component | Thin C exports over testable Go Helm operation packages | Java and Go share a JSON protocol by convention, but the protocol is not explicit enough. |

Useful existing traits to preserve:

- Java model types are immutable records and sealed hierarchies where useful.
- Native concerns do not leak into current model types.
- Runtime discovery already uses `ServiceLoader`.
- The Go bridge keeps cgo exports thin and most behavior in testable internal packages.

Problems to solve:

- `helm4j-spi` is both a consumer module and a provider module.
- Registry operations are currently under the repository client, even though Helm chart repositories and OCI registries are different concepts.
- Several builders allow required inputs to be omitted until native execution time.
- `ChartSource` mixes chart resolution, credentials, TLS, signature verification, and prerelease policy.
- Failure behavior is inconsistent: some operations return sealed success/failure results, while others throw.
- Java-to-Go protocol fields are built through string-keyed maps, making contract drift hard to detect.

## Design Principles

1. Consumer clarity wins over compatibility.
2. Artifact names should explain what a dependency gives a Gradle or Maven user.
3. JPMS module names should make `requires dev.nthings.helm4j;` mean "I use Helm4j."
4. SPI is provider-facing and should not be the normal application dependency.
5. Model types are public SDK vocabulary, not JSON or native protocol objects.
6. Required inputs should be explicit at construction time.
7. Runtime implementations should be swappable without leaking transport details.
8. The Java/Go protocol should be typed, documented, and fixture-tested.
9. No package should be split across modules.

## Target Module Graph

```
applications
    |
    v
helm4j-client             helm4j-testing
module dev.nthings.helm4j     |
    |                         |
    | requires                | requires
    v                         v
helm4j-spi  -------------------
module dev.nthings.helm4j.spi
    |
    | requires transitive
    v
helm4j-model
module dev.nthings.helm4j.model

helm4j-runtime-native
module dev.nthings.helm4j.runtime.ffm
    |
    | provides HelmEngineProvider
    v
helm4j-spi

libhelm4j
Go/cgo shared library used only by helm4j-runtime-native
```

`helm4j-runtime-native` should use module name `dev.nthings.helm4j.runtime.ffm`, not `dev.nthings.helm4j.runtime.native`, because `native` is a Java keyword and should not be used as a JPMS module segment.

## Target Modules

### `helm4j-model`

JPMS module: `dev.nthings.helm4j.model`

Audience: every Helm4j consumer and every runtime provider.

Responsibilities:

- Public domain vocabulary.
- Request and response records.
- Typed identifiers and configuration values.
- Error and result abstractions.
- Common immutable collection wrappers where they add API value.

Dependencies:

- `requires transitive org.jspecify`
- No client, SPI, Jackson, SLF4J, FFM, jextract, or Go bridge dependency.

Recommended package ownership:

- `dev.nthings.helm4j.chart`
- `dev.nthings.helm4j.release`
- `dev.nthings.helm4j.repository`
- `dev.nthings.helm4j.registry`
- `dev.nthings.helm4j.system`
- `dev.nthings.helm4j.values`
- `dev.nthings.helm4j.errors`
- `dev.nthings.helm4j.model` for truly generic SDK model helpers

Do not keep root-package model types in `dev.nthings.helm4j`; reserve that root package for the consumer client module. For example, move current `VersionInfo` to `dev.nthings.helm4j.system.VersionInfo`.

### `helm4j-client`

JPMS module: `dev.nthings.helm4j`

Audience: application developers.

Responsibilities:

- The default consumer entry point.
- Client lifecycle and configuration.
- Operation namespaces.
- ServiceLoader runtime discovery.
- Throwing-by-default execution behavior.
- Optional generic result capture helpers.

Dependencies:

- `requires transitive dev.nthings.helm4j.model`
- `requires dev.nthings.helm4j.spi`
- No native, Jackson, jextract, or Go dependency.

Primary packages:

- `dev.nthings.helm4j` for `Helm`, `HelmClient`, `HelmClientOptions`, and generic helpers such as `HelmResult`.
- `dev.nthings.helm4j.client` or `dev.nthings.helm4j.client.*` for namespace client implementation types when those types must be public return types.

Consumer dependency:

```kotlin
dependencies {
    implementation("dev.nthings.helm4j:helm4j-client")
    runtimeOnly("dev.nthings.helm4j:helm4j-runtime-native")
}
```

JPMS consumer module:

```java
module my.application {
  requires dev.nthings.helm4j;
}
```

The client may include an advanced engine-injection hook for tests and alternate runtimes, but examples and normal documentation should center `HelmClient.create()`. If a method accepts an SPI type, keep `dev.nthings.helm4j.spi` as a non-transitive client dependency so normal consumers are not forced to read SPI unless they opt into advanced runtime injection.

### `helm4j-spi`

JPMS module: `dev.nthings.helm4j.spi`

Audience: runtime implementors and advanced tests.

Responsibilities:

- Provider contract discovered through `ServiceLoader`.
- Runtime engine contract.
- Domain gateway contracts.
- Capability metadata exposed by a runtime.

Dependencies:

- `requires transitive dev.nthings.helm4j.model`
- No dependency on `helm4j-client`.

Public SPI shape:

```java
public interface HelmEngineProvider {
  String id();

  HelmEngine create(HelmEngineConfig config);
}

public interface HelmEngine extends AutoCloseable {
  ReleaseGateway releases();

  ChartGateway charts();

  RepositoryGateway repositories();

  RegistryGateway registries();

  SystemGateway system();

  @Override
  void close();
}
```

Do not keep the current aggregate shape where one `HelmGateway` extends every domain gateway. An explicit engine with sub-gateways preserves domain ownership and makes partial, fake, process-backed, or remote runtimes easier to reason about.

### `helm4j-runtime-native`

JPMS module: `dev.nthings.helm4j.runtime.ffm`

Audience: runtime path only for normal consumers.

Responsibilities:

- Default `HelmEngineProvider`.
- FFM transport to `libhelm4j`.
- Native library lookup.
- jextract bindings.
- Java model to native protocol mapping.
- Native protocol response decoding.
- Native runtime diagnostics.

Dependencies:

- `requires dev.nthings.helm4j.spi`
- `requires dev.nthings.helm4j.model`
- Jackson for internal JSON protocol.
- SLF4J for runtime logging.

Rules:

- Export no generated binding packages.
- Export no protocol internals.
- Keep ServiceLoader provider public only as required by JPMS.
- Split internals by responsibility, for example:
  - `internal.gateway`
  - `internal.protocol`
  - `internal.ffi`
  - `internal.library`
  - `internal.mapping`

Replace string-keyed `Map<String, Object>` protocol construction with typed internal protocol records. The mapping should be:

```
public model request -> internal protocol request -> JSON -> libhelm4j
libhelm4j JSON -> internal protocol response -> public model response
```

### `helm4j-testing`

JPMS module: `dev.nthings.helm4j.testing`

Audience: application tests and Helm4j contract tests.

Target responsibilities:

- Fake and stub `HelmEngine` implementations.
- Request capture for assertions.
- Scripted responses and failures.
- Contract tests that every runtime provider can reuse.

This module belongs in the target architecture, but it should be implemented after the model, client, SPI, and native runtime contracts stop moving. Adding it too early will force churn while `HelmEngine` is still settling.

### `helm4j-samples`

Audience: examples only.

Responsibilities:

- Compile against `helm4j-client`, the same artifact applications use.
- Put `helm4j-runtime-native` on the runtime path.
- Demonstrate the public API, not SPI or native internals.

### `libhelm4j`

Audience: implementation detail of `helm4j-runtime-native`.

Responsibilities:

- CGo shared-library boundary.
- Thin `//export` functions.
- Panic recovery and structured error envelope.
- Delegation to testable Go internal operation packages.

Rules:

- Keep cgo exports thin.
- Keep Helm behavior in Go internal packages.
- Keep the Java-facing JSON protocol stable and fixture-tested.

## Public Consumer API

The API should read as a Java SDK, not as a direct CLI flag mirror.

Default usage:

```java
try (var helm = HelmClient.create()) {
  var request =
      InstallRelease.builder(ReleaseName.of("nginx"), ChartRef.repo("bitnami/nginx"))
          .namespace(Namespace.of("apps"))
          .createNamespace(true)
          .values(Values.of(Map.of("service", Map.of("type", "ClusterIP"))))
          .build();

  Release release = helm.releases().install(request);
  ReleaseStatusView status = helm.releases().status(ReleaseName.of("nginx"), Namespace.of("apps"));
  ListResult<ChartSummary> charts = helm.charts().searchRepository(SearchCharts.keyword("nginx"));
}
```

Namespaces:

| Namespace | Responsibility |
| --- | --- |
| `helm.releases()` | Install, upgrade, uninstall, status, list, history, rollback, test, get values, get manifest, get hooks, get notes, get metadata |
| `helm.charts()` | Search, show, template, lint, pull, push, package, dependency operations |
| `helm.repositories()` | Add, update, list, and remove chart repositories |
| `helm.registries()` | OCI registry login and logout |
| `helm.system()` | Version, runtime capabilities, environment diagnostics |

Naming guidelines:

- Use plural namespace methods: `releases()`, `charts()`, `repositories()`, `registries()`, `system()`.
- Prefer operation model names that describe SDK intent:
  - `InstallRelease` instead of `InstallRequest`
  - `UpgradeRelease` instead of `UpgradeRequest`
  - `AddRepository` instead of `RepoAddRequest`
  - `SearchCharts` instead of separate CLI-shaped search request names where practical
- Keep Helm terminology where it is already a domain concept: release, chart, repository, registry, values, hooks, manifest, revision.

## Request Construction

Required inputs should be required at construction time.

Recommended shape:

```java
InstallRelease.builder(ReleaseName.of("nginx"), ChartRef.repo("bitnami/nginx"))
    .namespace(Namespace.of("apps"))
    .timeout(Duration.ofMinutes(3))
    .build();

AddRepository.of(RepositoryName.of("bitnami"), URI.create("https://charts.bitnami.com/bitnami"));

StatusRelease.of(ReleaseName.of("nginx")).namespace(Namespace.of("apps"));
```

Builders should handle optional flags only. This prevents the common current failure mode where `build()` or native execution discovers that a required release name, chart, or repository name was never provided.

## Value Types

Use value types where they prevent common bugs, centralize validation, or carry repeated domain meaning.

First-class value types:

| Type | Reason |
| --- | --- |
| `ReleaseName` | Reused across lifecycle, status, get, history, rollback, test, and uninstall operations |
| `Namespace` | Reused across most release operations and easy to accidentally swap with names |
| `RepositoryName` | Reused by repository add, update, list filtering, and remove |
| `RegistryHost` | Distinct from repository URL and chart reference |
| `ChartRef` | Core typed chart identity for repository, OCI, and local charts |
| `ChartVersion` | Reused with remote chart references and chart metadata |
| `Values` | Gives a stable abstraction over maps, YAML, and future typed values input |
| `Credentials` | Groups username/password or token-bearing auth data |
| `TlsOptions` | Groups certificate, key, CA, plain HTTP, and insecure TLS settings |

Keep these as plain JDK types unless a stronger domain need appears:

| Shape | Recommendation |
| --- | --- |
| Timeouts | `Duration` |
| Filesystem locations | `Path` |
| Repository URLs and API endpoints | `URI` |
| Descriptions, notes, manifests, YAML | `String` |
| Labels and annotations | `Map<String, String>` initially |
| Lists of event names, API versions, repository names | `List<String>` or lists of the stronger type where already available |

Avoid wrapping every scalar. The goal is to prevent meaningful category mistakes, not to make the API ceremonial.

## Chart Identity And Resolution

Split chart identity from chart resolution.

`ChartRef` answers "which chart?"

Examples:

```java
ChartRef.repo("bitnami/nginx").version(ChartVersion.of("19.0.0"));
ChartRef.oci("oci://registry.example.com/charts/nginx").version(ChartVersion.of("19.0.0"));
ChartRef.local(Path.of("charts/hello-world"));
```

`ChartResolution` answers "how should Helm resolve or fetch it?"

```java
ChartResolution resolution =
    ChartResolution.builder()
        .repository(URI.create("https://charts.bitnami.com/bitnami"))
        .credentials(Credentials.basic("user", "password"))
        .tls(TlsOptions.insecure())
        .includePreReleases(false)
        .verifySignatures(true)
        .build();
```

Request types that consume charts can then accept both:

```java
InstallRelease.builder(ReleaseName.of("nginx"), ChartRef.repo("bitnami/nginx"))
    .resolution(resolution)
    .build();
```

This replaces the current broad `ChartSource` concept with smaller responsibilities:

- `ChartRef`: identity and version.
- `ChartResolution`: repository URL, prerelease policy, signature policy.
- `Credentials`: authentication.
- `TlsOptions`: transport security.

## Error And Result Semantics

Default namespace methods should throw typed exceptions on Helm command failure or runtime failure.

Recommended exception hierarchy:

```java
HelmException
  HelmCommandException
  HelmRuntimeException
  HelmConfigurationException
```

`HelmCommandException` carries a `HelmFailure` with message, operation, stage, and any runtime-provided context.

Default:

```java
Release release = helm.releases().install(request);
```

No-throw capture:

```java
HelmResult<Release> result =
    HelmResult.capture(() -> helm.releases().install(request));
```

Do not add `tryInstall`, `tryUpgrade`, `tryStatus`, and similar duplicate methods in the initial API. A generic `HelmResult.capture(...)` helper is enough until real consumers show that fluent no-throw methods are worth the surface area.

Mutation operations should return domain values:

- Install and upgrade return `Release`.
- Uninstall returns `UninstallReport`.
- Rollback returns `RollbackReport` or the resulting `Release`, depending on what Helm reliably reports.
- Pending is represented by `Release.status()`, not by a separate result subtype.

## SPI Design

SPI is a runtime-provider contract. It should not know about fluent consumer builders or client namespace classes.

Provider:

```java
public interface HelmEngineProvider {
  String id();

  HelmEngine create(HelmEngineConfig config);
}
```

Engine:

```java
public interface HelmEngine extends AutoCloseable {
  ReleaseGateway releases();

  ChartGateway charts();

  RepositoryGateway repositories();

  RegistryGateway registries();

  SystemGateway system();
}
```

Gateway examples:

```java
public interface ReleaseGateway {
  Release install(InstallRelease request);

  Release upgrade(UpgradeRelease request);

  UninstallReport uninstall(UninstallRelease request);

  ReleaseStatusView status(StatusRelease request);
}

public interface RegistryGateway {
  RegistrySession login(RegistryLogin request);

  RegistryLogoutReport logout(RegistryLogout request);
}
```

Rules:

- Gateways accept model requests and return model responses.
- Gateways do not depend on `HelmClient`.
- Gateways do not expose JSON, FFM, cgo, Jackson, or jextract types.
- SPI failures should use the same `HelmException` family as the client.

## Runtime Selection And Configuration

Normal path:

```java
try (var helm = HelmClient.create()) {
  // Uses the default provider discovered through ServiceLoader.
}
```

Configured path:

```java
var options =
    HelmClientOptions.builder()
        .runtime("native")
        .kubeContext("kind-dev")
        .build();

try (var helm = HelmClient.create(options)) {
  // Uses the selected provider and passes supported options to the engine.
}
```

Advanced injection, if retained:

```java
try (var helm = HelmClient.using(engine)) {
  // Explicit test or alternate-runtime path.
}
```

This method should be documented as advanced. Primary examples should not teach SPI as the normal way to create a client.

## Native Runtime Design

Target internal native runtime layers:

```
FfmHelmEngineProvider
    -> NativeHelmEngine
        -> NativeReleaseGateway
        -> NativeChartGateway
        -> NativeRepositoryGateway
        -> NativeRegistryGateway
        -> NativeSystemGateway
            -> ProtocolMapper
            -> ProtocolCodec
            -> HelmBridge
                -> FfmHelmBridge
                    -> jextract bindings
                    -> libhelm4j.so
```

Responsibilities:

- Gateways map SDK model requests to internal protocol records.
- Protocol records define the JSON payload contract.
- Codec serializes/deserializes only protocol records.
- Bridge sends and receives UTF-8 bytes.
- FFM layer owns memory segments and native free behavior.
- Library layer owns lookup and diagnostics.

Protocol records should be named by operation, for example:

- `InstallCommand`
- `InstallResponse`
- `SearchRepositoryCommand`
- `RepositoryUpdateResponse`
- `OperationErrorEnvelope`

Every protocol record should have fixtures that can be validated from Java and Go.

## Java/Go Protocol Contract

The Java and Go sides should share a documented protocol directory, for example:

```
protocol/
  install-command.json
  install-response.json
  operation-error.json
  repository-update-command.json
```

Contract tests:

- Java serializes protocol records to the golden JSON fixtures.
- Java deserializes Go-shaped fixtures into protocol responses.
- Go parses Java-shaped fixtures into operation options.
- Go serializes responses matching Java fixtures.
- Error envelopes preserve `stage`, `operation`, `message`, and context fields.

The existing `check-native-parity` task should continue to verify that Go exports, headers, jextract bindings, and Java bridge methods stay in sync. Add protocol fixtures as a separate gate because native symbol parity does not prove payload compatibility.

## Refactoring Plan

### Phase 1: Freeze The Target Contracts

- Commit this design as the architectural target.
- Decide final package names for model and client namespace types.
- Write small API sketches or compile-only tests for the desired consumer examples.
- Define the exception/result policy before changing runtime behavior.

### Phase 2: Create `helm4j-model`

- Rename `helm4j-api` to `helm4j-model`.
- Change JPMS module to `dev.nthings.helm4j.model`.
- Move root model types out of `dev.nthings.helm4j`.
- Rename `repo` package to `repository`.
- Split OCI registry types into `registry`.
- Add first-class value types for the agreed identifiers and configuration bundles.
- Replace required optional-builder fields with constructors or builders that require mandatory arguments up front.

### Phase 3: Split Client From SPI

- Create `helm4j-client` with JPMS module `dev.nthings.helm4j`.
- Move `Helm`, `HelmClient`, and namespace clients out of `helm4j-spi`.
- Rename namespace methods to `releases()`, `charts()`, `repositories()`, `registries()`, and `system()`.
- Keep `helm4j-spi` provider-only.
- Update samples to depend on `helm4j-client`.

### Phase 4: Replace The Aggregate Gateway

- Replace `HelmGateway extends RepoGateway, ChartGateway, ReleaseGateway, SystemGateway` with `HelmEngine`.
- Split repository and registry gateways.
- Make `HelmClient` wrap `HelmEngine`.
- Keep direct engine injection advanced and out of quick-start examples.

### Phase 5: Standardize Failure Handling

- Introduce the typed exception hierarchy.
- Make namespace methods throw by default.
- Replace sealed mutation failure results with domain values plus exceptions.
- Add `HelmResult.capture(...)` for no-throw workflows.

### Phase 6: Refactor The Native Runtime

- Rename `helm4j-native` to `helm4j-runtime-native`.
- Change JPMS module to `dev.nthings.helm4j.runtime.ffm`.
- Create internal runtime packages by responsibility.
- Replace `NativeOptions` map construction with typed protocol records.
- Add Java/Go protocol fixtures and tests.
- Keep generated jextract bindings non-exported.

### Phase 7: Add Testing Support

- Create `helm4j-testing` after SPI stabilizes.
- Provide fake engines, scripted gateways, and request capture.
- Move ad hoc test fake implementations into the testing module.
- Add reusable SPI contract tests for runtime providers.

### Phase 8: Refresh Documentation And Samples

- Rewrite README quick start around `helm4j-client`.
- Update architecture docs with the new module graph.
- Expand public API docs with consumer examples.
- Keep SPI and runtime-provider docs separate from user quick starts.

## Rejected Alternatives

### Only Rename `helm4j-spi` To `helm4j-client`

This improves dependency naming, but it keeps consumer classes and provider contracts in one module. The main boundary problem remains.

### Merge All Java Code Into One Module

This gives the shortest dependency story, but makes it too easy for Jackson, FFM, jextract, and native bridge details to leak into the public API.

### Publish Per-Domain Java Modules

Examples: `helm4j-release`, `helm4j-chart`, `helm4j-repository`.

This is too granular for the current project. Helm operations are related enough that per-domain modules would add dependency friction without clear consumer benefit.

### Make The SPI A Generic Command Executor

Example: `execute(Command<T>)`.

This is flexible, but it weakens domain boundaries and centralizes too much dispatch logic in one abstraction. Domain gateways are clearer for runtime implementors and easier to test.

### Generate The Java API Directly From Go Protocol Types

This would reduce some duplication, but it would make the consumer API follow native wire concerns. The model should be designed for Java consumers first; protocol types are internal mapping details.

## Final Target

The final consumer story should be:

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
              InstallRelease.builder(ReleaseName.of("nginx"), ChartRef.repo("bitnami/nginx"))
                  .namespace(Namespace.of("apps"))
                  .createNamespace(true)
                  .build());
}
```

The final architecture should make these boundaries obvious:

- `helm4j-client` is what applications use.
- `helm4j-model` is the shared public vocabulary.
- `helm4j-spi` is what runtimes implement.
- `helm4j-runtime-native` is one runtime provider.
- `helm4j-testing` is for tests, fakes, and contract verification.
- `libhelm4j` is an implementation detail behind the native runtime.
