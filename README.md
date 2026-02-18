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
import dev.nthings.helm4j.repo.RepoAddSuccess;
import dev.nthings.helm4j.release.InstallSuccess;
import dev.nthings.helm4j.types.ChartRef;

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

  var metadata = helm.chart().chart(ChartRef.repo("bitnami/nginx"), spec -> {});
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
                      .timeout(Duration.ofMinutes(5))
                      .values(Map.of("service", Map.of("type", "ClusterIP"))));

  if (install instanceof InstallSuccess success) {
    System.out.println(success.release().status());
  }
}
```

## Public API

- `Helm.client()`
- `Helm.client(spec -> ...)`
- `HelmClient.repo().add(...)`, `.update(...)`, `.list()`, `.remove(...)` using request/spec overloads
- `HelmClient.chart().searchRepo(...)`, `.searchHub(...)` using request/spec overloads
- `HelmClient.chart().chart(...)`, `.values(...)`, `.readme(...)`, `.crds(...)`, `.all(...)`
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

### Build Native Library

```bash
cd libhelm4j
GO111MODULE=on go build -buildmode=c-shared -o libhelm4j.so .
```

### Build Java SDK

```bash
./gradlew build
```

### Run Checks

```bash
./gradlew check
cd libhelm4j && go test ./...
```

## Documentation

- `docs/spec.md`: architecture and API specification
- `docs/feature-parity.md`: implemented vs planned Helm v4 action coverage

## License

See `LICENSE`.
