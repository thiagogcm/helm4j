# Helm4j Feature Parity (Standard SDK)

This document tracks Helm v4 action coverage in the new standard SDK surface.

## Legend

- ✅ Supported in standard SDK
- 🚧 Planned
- ❌ Not implemented

## Public Surface Tracked

- `Helm.client().repo()`
- `Helm.client().chart()`
- `Helm.client().release()`
- `Helm.client().version()`
- `Helm.install(...)` / `Helm.upgrade(...)` (fluent one-shot builders)
- `Helm.version()`

## Action Matrix

| Helm Action | CLI Command | Standard SDK API | Status |
| --- | --- | --- | --- |
| `repo add` | `helm repo add NAME URL` | `repo().add(...)` | ✅ |
| `repo update` | `helm repo update [REPO...]` | `repo().update(...)` | ✅ |
| `repo list` | `helm repo list` | `repo().list()` | ✅ |
| `repo remove` | `helm repo remove NAME...` | `repo().remove(...)` | ✅ |
| `search repo` | `helm search repo [KEYWORD]` | `chart().searchRepo(...)` | ✅ |
| `search hub` | `helm search hub [KEYWORD]` | `chart().searchHub(...)` | ✅ |
| `show chart` | `helm show chart CHART` | `chart().chart(...)` | ✅ |
| `show values` | `helm show values CHART` | `chart().values(...)` | ✅ |
| `show readme` | `helm show readme CHART` | `chart().readme(...)` | ✅ |
| `show crds` | `helm show crds CHART` | `chart().crds(...)` | ✅ |
| `show all` | `helm show all CHART` | `chart().all(...)` | ✅ |
| `install` | `helm install RELEASE CHART` | `release().install(...)` | ✅ |
| `upgrade` | `helm upgrade RELEASE CHART` | `release().upgrade(...)` | ✅ |
| `rollback` | `helm rollback RELEASE [REVISION]` | `release().rollback(...)` | ✅ |
| `uninstall` | `helm uninstall RELEASE` | `release().uninstall(...)` | ✅ |
| `status` | `helm status RELEASE` | `release().status(...)` | ✅ |
| `history` | `helm history RELEASE` | `release().history(...)` | ✅ |
| `get all` | `helm get all RELEASE` | `release().getAll(...)` | ✅ |
| `get values` | `helm get values RELEASE` | `release().getValues(...)` | ✅ |
| `get manifest` | `helm get manifest RELEASE` | `release().getManifest(...)` | ✅ |
| `get hooks` | `helm get hooks RELEASE` | `release().getHooks(...)` | ✅ |
| `get notes` | `helm get notes RELEASE` | `release().getNotes(...)` | ✅ |
| `get metadata` | `helm get metadata RELEASE` | `release().getMetadata(...)` | ✅ |
| `template` | `helm template RELEASE CHART` | `chart().template(...)` | ✅ |
| `lint` | `helm lint PATH` | `chart().lint(...)` | ✅ |
| `version` | `helm version` | `version()` | ✅ |

## Native Bridge Coverage

All implemented operations execute through the JSON-native bridge
(`HelmRepo`, `HelmSearch`, `HelmShow`, `HelmInstall`, `HelmUpgrade`,
`HelmUninstall`, `HelmStatus`, `HelmRollback`, `HelmHistory`, `HelmGet`,
`HelmTemplate`, `HelmLint`, `HelmVersion`).

## Helm v4-Specific Notes

- Server-side apply is exposed through `ApplyStrategy` and defaults to `SERVER_SIDE_APPLY` in `InstallRequest` and `UpgradeRequest`.
- OCI references are first-class through `ChartRef.oci(...)`.
- Release install domain outcomes are modeled with sealed results:
  - `InstallSuccess` / `InstallPending` / `InstallFailure`
  - `UpgradeSuccess` / `UpgradePending` / `UpgradeFailure`
  - `UninstallSuccess` / `UninstallFailure`
  - `RollbackSuccess` / `RollbackFailure`
