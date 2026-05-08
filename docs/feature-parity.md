# Helm4j Feature Parity

This document tracks Helm v4 action coverage in Helm4j.

## Helm v4 SDK pin

`libhelm4j` currently pins `helm.sh/helm/v4 v4.1.4`. Wait strategies (`watcher`, `legacy`,
`hookOnly`) defined in `helm.sh/helm/v4/pkg/kube` are exposed through the `WaitMode` enum
on `InstallRequest`, `UpgradeRequest`, `RollbackRequest`, and `UninstallRequest`. Unknown
wait values are rejected at the Go side and surface as an error payload across the FFM
boundary.

## Intentionally not exposed

- **`--no-headers` for `helm repo list`**: a CLI-only formatting flag. The SDK already
  returns structured `RepoSummary` records, so suppressing a tabwriter header has no
  meaning at the API level.
- **Custom `kstatus` readers**: Helm v4.1 lets Go callers plug in custom
  `engine.StatusReader` implementations to influence wait-for-ready semantics. Each reader
  receives live `*unstructured.Unstructured` Kubernetes objects synchronously inside the
  watcher loop, which cannot be marshalled across the JSON-only FFM boundary without
  reverse JVM upcalls. Callers that need this should embed `libhelm4j` as a Go module
  rather than going through the Java SDK.
