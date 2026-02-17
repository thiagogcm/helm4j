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
| `upgrade` | `helm upgrade RELEASE CHART` | - | ❌ |
| `rollback` | `helm rollback RELEASE [REVISION]` | - | ❌ |
| `uninstall` | `helm uninstall RELEASE` | - | ❌ |
| `status` | `helm status RELEASE` | - | ❌ |
| `history` | `helm history RELEASE` | - | ❌ |
| `get` | `helm get ...` | - | ❌ |
| `template` | `helm template ...` | - | ❌ |
| `lint` | `helm lint ...` | - | ❌ |

## Native Bridge Coverage

All implemented operations above execute through the JSON-native bridge
(`HelmRepo`, `HelmSearch`, `HelmShow`, `HelmInstall`).

## Helm v4-Specific Notes

- Server-side apply is exposed through `ApplyStrategy` and defaults to `SERVER_SIDE_APPLY` in `InstallRequest`.
- OCI references are first-class through `ChartRef.oci(...)`.
- Release install domain outcomes are modeled with sealed results:
  - `InstallSuccess`
  - `InstallPending`
  - `InstallFailure`
