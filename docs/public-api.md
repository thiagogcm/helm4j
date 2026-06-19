# Public API

`HelmClient` is the application entry point. A client is safe to share across threads and should be closed with try-with-resources.

```java
try (var helm = HelmClient.create()) {
  var result = helm.charts().lint(b -> b.chartPath(chartPath).strict(true));
}
```

## Client creation

| Entry point                  | Use                                                           |
| ---------------------------- | ------------------------------------------------------------- |
| `HelmClient.create()`        | Load the first `HelmEngineProvider` found by `ServiceLoader`  |
| `HelmClient.create(options)` | Select a runtime ID and pass engine configuration             |
| `HelmClient.using(engine)`   | Use a supplied engine, usually in tests or alternate runtimes |

The bundled runtime ID is `native`. Its engine-level `kubeContext` and custom properties are not applied yet.

## Namespaces

Every request-based operation has two overloads: a `Consumer<Builder>` for inline configuration and a pre-built request for reuse.

### Releases

| Method        | Request            | Result                     |
| ------------- | ------------------ | -------------------------- |
| `install`     | `InstallRelease`   | `Release`                  |
| `upgrade`     | `UpgradeRelease`   | `Release`                  |
| `uninstall`   | `UninstallRelease` | `UninstallReport`          |
| `status`      | `StatusRelease`    | `Release`                  |
| `rollback`    | `RollbackRelease`  | `RollbackReport`           |
| `history`     | `ReleaseHistory`   | `ListResult<HistoryEntry>` |
| `list`        | `ListReleases`     | `ListResult<Release>`      |
| `test`        | `TestRelease`      | `TestResult`               |
| `getAll`      | `GetRelease`       | `GetAllResult`             |
| `getValues`   | `GetRelease`       | `GetValuesResult`          |
| `getManifest` | `GetRelease`       | `GetManifestResult`        |
| `getHooks`    | `GetRelease`       | `GetHooksResult`           |
| `getNotes`    | `GetRelease`       | `GetNotesResult`           |
| `getMetadata` | `GetRelease`       | `GetMetadataResult`        |

Release mutations expose Helm v4 wait modes through `WaitMode` and apply behavior through `ApplyStrategy`. Pending work is represented by `Release.status()`.

### Charts

| Method             | Request                               | Result                         |
| ------------------ | ------------------------------------- | ------------------------------ |
| `searchRepository` | `SearchCharts`                        | `ListResult<RepoChartSummary>` |
| `searchHub`        | `SearchHub`                           | `ListResult<HubChartSummary>`  |
| `show`             | `ShowMode`, `ChartRef`, `ShowRequest` | `ShowResult`                   |
| `template`         | `TemplateRequest`                     | `TemplateResult`               |
| `lint`             | `LintRequest`                         | `LintResult`                   |
| `pull`             | `PullRequest`                         | `PullResult`                   |
| `push`             | `PushRequest`                         | `PushResult`                   |
| `packageChart`     | `PackageChartRequest`                 | `PackageChartResult`           |
| `dependency`       | `DependencyRequest`                   | `DependencyResult`             |

`ChartRef` distinguishes repository, OCI, and local charts:

```java
ChartRef.repo("bitnami/nginx");
ChartRef.repo("bitnami/nginx", "21.0.0");
ChartRef.oci("oci://registry.example.com/team/chart");
ChartRef.local(Path.of("charts/my-chart"));
```

Credentials, TLS, and repository lookup settings belong to `ChartSource`. `dependency` currently performs `helm dependency list`; build and update are not exposed by `DependencyRequest`.

### Repositories

| Method   | Request              | Result                              |
| -------- | -------------------- | ----------------------------------- |
| `add`    | `AddRepository`      | `AddRepositoryReport`               |
| `update` | `UpdateRepositories` | `ListResult<RepositoryUpdateEntry>` |
| `list`   | none                 | `ListResult<RepositorySummary>`     |
| `remove` | `RemoveRepository`   | `ListResult<String>`                |

### Registries and system

| Namespace      | Method    | Request          | Result           |
| -------------- | --------- | ---------------- | ---------------- |
| `registries()` | `login`   | `RegistryLogin`  | `RegistryResult` |
| `registries()` | `logout`  | `RegistryLogout` | `RegistryResult` |
| `system()`     | `version` | none             | `VersionInfo`    |

## Results and failures

Methods return typed records and throw on failure:

- `HelmConfigurationException` for invalid client or request configuration;
- `HelmCommandException` when Helm rejects or cannot complete an operation;
- `HelmRuntimeException` for provider, native library, FFM, or protocol failures.

All three extend `HelmException`. `HelmCommandException.failure()` carries a `HelmFailure` with message, stage, and operation fields.

Use `HelmResult.capture` when an explicit result is preferable:

```java
var result = HelmResult.capture(() -> helm.releases().status(request));
if (result.isOk()) {
  use(result.value());
} else {
  report(result.error());
}
```

`ListResult<T>` is immutable and implements `Iterable<T>`; it also provides `size()`, `isEmpty()`, `stream()`, and `first()`.
