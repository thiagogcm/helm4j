# Helm4j

Helm4j is a Java SDK for Helm v4 focused on idiomatic Java APIs and a stable native bridge.

## Highlights

- Standard entrypoint: `Helm.client()`
- Discoverable namespaces: `repo()`, `chart()`, `release()`
- Fluent, terminal request builders: configure a call and `execute()` it in one chain
- Immutable API models with Java records
- Sealed result types, one per operation family
- JDK 25+ FFM bridge over JSON C exports from `libhelm4j`

## Quick Start

Every operation on a namespace client returns a fluent request builder. Configure it inline and
call `execute()` to run it.

```java
import java.time.Duration;
import java.util.Map;

import dev.nthings.helm4j.Helm;
import dev.nthings.helm4j.chart.ChartRef;
import dev.nthings.helm4j.chart.ShowMode;
import dev.nthings.helm4j.release.ReleaseSuccess;
import dev.nthings.helm4j.release.WaitMode;
import dev.nthings.helm4j.repo.RepoAddSuccess;

try (var helm = Helm.client()) {
  var add =
      helm.repo()
          .add()
          .name("bitnami")
          .url("https://charts.bitnami.com/bitnami")
          .execute();

  if (add instanceof RepoAddSuccess success) {
    System.out.println("Added repo: " + success.name());
  }

  var search =
      helm.chart().searchRepo().keyword("nginx").includeAllVersions(true).execute();
  search.first().ifPresent(chart -> System.out.println(chart.name()));

  var hub = helm.chart().searchHub().keyword("nginx").execute();
  hub.first().ifPresent(chart -> System.out.println(chart.url()));

  var metadata = helm.chart().show(ShowMode.CHART, ChartRef.repo("bitnami/nginx")).execute();
  System.out.println(metadata.metadataYaml());

  var repos = helm.repo().list();
  System.out.println("Configured repos: " + repos.size());

  var install =
      helm.release()
          .install()
          .releaseName("nginx")
          .chart(ChartRef.repo("bitnami/nginx"))
          .namespace("apps")
          .createNamespace(true)
          .waitMode(WaitMode.HOOK_ONLY)
          .timeout(Duration.ofMinutes(5))
          .values(Map.of("service", Map.of("type", "ClusterIP")))
          .execute();

  if (install instanceof ReleaseSuccess success) {
    System.out.println(success.release().status());
  }
}
```

A request can also be built once with `Request.builder()` and reused by passing it to the
matching `operation(Request)` overload — useful when the same request is issued repeatedly.

### Working with sealed result types

Each lifecycle operation returns its own sealed result type, so an exhaustive `switch` only ever
sees outcomes that operation can actually produce:

- `install` / `upgrade` return `ReleaseResult` — `ReleaseSuccess`, `ReleasePending`, `ReleaseFailure`
- `uninstall` returns `UninstallResult` — `UninstallSuccess`, `UninstallFailure`
- `rollback` returns `RollbackResult` — `RollbackSuccess`, `RollbackFailure`

Prefer a `switch` expression with type patterns over `instanceof` chains — the compiler enforces
exhaustiveness, so adding a new permit later forces every call site to be updated:

```java
String describe(ReleaseResult result) {
  return switch (result) {
    case ReleaseSuccess s -> "ok " + s.release().status();
    case ReleasePending p -> "pending " + p.release().status();
    case ReleaseFailure f -> "failed " + f.message();
  };
}
```

Failures across both the value channel (`ReleaseFailure`, `UninstallFailure`, `RollbackFailure`,
`RepoAddFailure`) and the exception channel (`HelmException`) are described by the same
`HelmFailure` carrier. `RepoAddResult` follows the same sealed-result pattern.

### Failure model

- Lifecycle mutations (`install`, `upgrade`, `uninstall`, `rollback`, `repo add`) report failure
  as a value: a `*Failure` permit of the operation's sealed result.
- Inspection and read operations (`status`, `list`, `get*`, `search*`, `show`, `template`,
  `lint`, repository updates, registry login/logout) report failure by throwing `HelmException`.

## Public API

- `Helm.client()` — open a client
- `helm.repo().add() / update() / list() / remove() / registryLogin() / registryLogout()`
- `helm.chart().searchRepo() / searchHub() / show(mode, chart) / template() / lint() / pull() / push() / packageChart() / dependency()`
- `helm.release().install() / upgrade() / uninstall() / status() / rollback() / history() / list() / test() / get()`

Each builder-returning method has a sibling `operation(Request)` overload that accepts a
pre-built request. `helm.release().get()` selects its variant via a terminal method on the
builder: `all()`, `values()`, `manifest()`, `hooks()`, `notes()` or `metadata()`.

## Native Bridge

The Java gateway uses JSON-based native exports from `libhelm4j`:

- `HelmRepo(mode, optionsJson)`
- `HelmSearch(mode, optionsJson)`
- `HelmShow(mode, chartRef, optionsJson)`
- `HelmInstall(releaseName, chartRef, optionsJson)`

Native strings are released with:

- `FreeString`

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

## Documentation

- `docs/spec.md`: architecture and API specification
- `docs/feature-parity.md`: implemented vs planned Helm v4 action coverage

## License

See `LICENSE`.
