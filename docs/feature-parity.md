# Feature parity

This page tracks operation-level coverage of the Helm v4.2.2 SDK pinned in `libhelm4j/go.mod`. “Supported” means the Java client, SPI, FFM runtime, cgo export, and Go operation are all connected. It does not claim parity with every CLI formatting flag.

## Supported operations

| Area            | Helm operations                                                            | Java namespace   | Status    |
| --------------- | -------------------------------------------------------------------------- | ---------------- | --------- |
| Releases        | install, upgrade, uninstall, status, rollback, history, list, test         | `releases()`     | Supported |
| Release data    | get all, values, manifest, hooks, notes, metadata                          | `releases()`     | Supported |
| Charts          | search repo, search hub, show chart/values/readme/CRDs/all, template, lint | `charts()`       | Supported |
| Chart artifacts | pull, push, package, dependency list                                       | `charts()`       | Supported |
| Repositories    | add, update, list, remove                                                  | `repositories()` | Supported |
| Registries      | login, logout                                                              | `registries()`   | Supported |
| Runtime         | version                                                                    | `system()`       | Supported |

All supported operations use structured requests and results. Helm v4 wait strategies (`WATCHER`, `LEGACY`, `HOOK_ONLY`), server-side apply choices, OCI chart references, credentials, and TLS options are represented in the model where the underlying action accepts them.

## Gaps

| Capability                                   | Current state                                                                      |
| -------------------------------------------- | ---------------------------------------------------------------------------------- |
| Dependency build and update                  | Implemented inside `libhelm4j`, but `DependencyRequest` exposes only list behavior |
| Engine-level Kubernetes context              | `HelmClientOptions.kubeContext()` is ignored by the native runtime                 |
| Provider-specific properties                 | Accepted by `HelmClientOptions`, but unused by the native runtime                  |
| Repository index, chart create, chart verify | Not exposed                                                                        |
| Helm plugins                                 | Not exposed                                                                        |
| Native distribution                          | Linux `libhelm4j.so` is built from source; no published platform artifacts         |

CLI presentation and shell integration commands such as completion, environment output, and generated command docs are intentionally outside the typed SDK surface.
