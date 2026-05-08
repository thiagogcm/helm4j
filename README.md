# Helm4j

Helm4j is a Java SDK for Helm v4 focused on idiomatic Java APIs and a stable native bridge.

## Highlights

- Standard entrypoint: `Helm.client()`
- Discoverable namespaces: `repo()`, `chart()`, `release()`
- Immutable API models with Java records
- Sealed result types for domain outcomes
- JDK 25+ FFM bridge over JSON C exports from `libhelm4j`

## Quick Start

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
          .add(spec -> spec.name("bitnami").url("https://charts.bitnami.com/bitnami"));

  if (add instanceof RepoAddSuccess success) {
    System.out.println("Added repo: " + success.name());
  }

  var search =
      helm.chart()
          .searchRepo(spec -> spec.keyword("nginx").includeAllVersions(true));
  search.first().ifPresent(chart -> System.out.println(chart.name()));

  var hub = helm.chart().searchHub(spec -> spec.keyword("nginx"));
  hub.first().ifPresent(chart -> System.out.println(chart.url()));

  var metadata = helm.chart().show(ShowMode.CHART, ChartRef.repo("bitnami/nginx"), spec -> {});
  System.out.println(metadata.metadataYaml());

  var repos = helm.repo().list();
  System.out.println("Configured repos: " + repos.size());

  var install =
      helm.release()
          .install(
              spec ->
                  spec.releaseName("nginx")
                      .chart(ChartRef.repo("bitnami/nginx"))
                      .namespace("apps")
                      .createNamespace(true)
                      .waitMode(WaitMode.HOOK_ONLY)
                      .timeout(Duration.ofMinutes(5))
                      .values(Map.of("service", Map.of("type", "ClusterIP"))));

  if (install instanceof ReleaseSuccess success) {
    System.out.println(success.release().status());
  }
}
```

### Working with sealed result types

Lifecycle operations return `ReleaseOutcome`, a sealed interface that permits exactly five
result types: `ReleaseSuccess`, `ReleasePending`, `ReleaseFailure`, `UninstallSuccess`, and
`RollbackSuccess`. When you forward a `ReleaseOutcome` through a generic helper that needs
to handle every variant, prefer a `switch` expression with type patterns over `instanceof`
chains — the compiler enforces exhaustiveness, so adding a new permit later forces every
call site to be updated:

```java
String describe(ReleaseOutcome outcome) {
  return switch (outcome) {
    case ReleaseSuccess s -> "ok " + s.release().status();
    case ReleasePending p -> "pending " + p.release().status();
    case ReleaseFailure f -> "failed " + f.message();
    case UninstallSuccess u -> "uninstalled " + u.releaseName();
    case RollbackSuccess r -> "rolled back to revision " + r.release().version();
  };
}
```

`RepoAddResult` and `LintResult` follow the same pattern.

## Public API

- `Helm.client()`
- `Helm.client(spec -> ...)`
- `HelmClient.repo().add(...)`, `.update(...)`, `.list()`, `.remove(...)` using request/spec overloads
- `HelmClient.chart().searchRepo(...)`, `.searchHub(...)` using request/spec overloads
- `HelmClient.chart().show(mode, ...)` for all show operations
- `HelmClient.release().install(...)`

### API Normalization

- Convenience scalar overloads were removed in favor of a consistent pair:
  - `operation(Request request)`
  - `operation(Consumer<Request.Builder> spec)`
- Default no-arg methods are kept only for semantically default actions (for example `repo().list()`, `repo().update()`, `release().list()`).

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
