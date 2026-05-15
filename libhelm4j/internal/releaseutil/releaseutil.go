// Package releaseutil provides shared release mapping utilities used by
// operations that return release information across the FFM boundary.
package releaseutil

import (
	"fmt"
	"iter"
	"time"

	"helm.sh/helm/v4/pkg/chart"
	"helm.sh/helm/v4/pkg/release"
	v1release "helm.sh/helm/v4/pkg/release/v1"
)

// IsAllUninstalled returns true when every release in the history has the
// "uninstalled" status, indicating the release was deleted and can be
// re-installed via an upgrade --install flow.
func IsAllUninstalled(releases []release.Releaser) bool {
	for _, r := range releases {
		acc, err := release.NewAccessor(r)
		if err != nil {
			return false
		}
		if acc.Status() != "uninstalled" {
			return false
		}
	}
	return true
}

// ReleaseInfo is the structured release payload returned on success.
type ReleaseInfo struct {
	Name          string            `json:"name"`
	Namespace     string            `json:"namespace"`
	Revision      int               `json:"revision"`
	Status        string            `json:"status"`
	Description   string            `json:"description"`
	FirstDeployed string            `json:"firstDeployed"`
	LastDeployed  string            `json:"lastDeployed"`
	ChartName     string            `json:"chartName"`
	ChartVersion  string            `json:"chartVersion"`
	AppVersion    string            `json:"appVersion"`
	Notes         string            `json:"notes"`
	Labels        map[string]string `json:"labels,omitempty"`
	ApplyMethod   string            `json:"applyMethod,omitempty"`
}

// MapRelease converts a Helm SDK [release.Releaser] into the serialisable
// [ReleaseInfo] returned across the FFM boundary. It uses the [release.Accessor]
// interface for forward-compatible field access, falling back to a type
// assertion for fields only available on the concrete v1 release type
// (Description, FirstDeployed).
func MapRelease(rel release.Releaser) (ReleaseInfo, error) {
	acc, err := release.NewAccessor(rel)
	if err != nil {
		return ReleaseInfo{}, fmt.Errorf("create release accessor: %w", err)
	}

	deployedAt := acc.DeployedAt()

	info := ReleaseInfo{
		Name:        acc.Name(),
		Namespace:   acc.Namespace(),
		Revision:    acc.Version(),
		Status:      acc.Status(),
		Notes:       acc.Notes(),
		Labels:      acc.Labels(),
		ApplyMethod: acc.ApplyMethod(),
	}

	if !deployedAt.IsZero() {
		info.LastDeployed = deployedAt.UTC().Format(time.RFC3339)
	}

	if ch := acc.Chart(); ch != nil {
		if chAcc, accErr := chart.NewAccessor(ch); accErr == nil {
			info.ChartName = chAcc.Name()
			meta := chAcc.MetadataAsMap()
			if v, ok := meta["Version"].(string); ok {
				info.ChartVersion = v
			}
			if v, ok := meta["AppVersion"].(string); ok {
				info.AppVersion = v
			}
		}
	}

	// Description and FirstDeployed are not exposed by release.Accessor;
	// they require direct access to the v1 release type.
	if v1, ok := rel.(*v1release.Release); ok && v1.Info != nil {
		info.Description = v1.Info.Description
		if !v1.Info.FirstDeployed.IsZero() {
			info.FirstDeployed = v1.Info.FirstDeployed.UTC().Format(time.RFC3339)
		}
	}

	return info, nil
}

// V1Hooks yields each hook in hooks unwrapped to its concrete *v1release.Hook
// shape. Hooks that are neither *v1release.Hook nor v1release.Hook are skipped.
//
// release.HookAccessor only exposes Path() and Manifest(); callers that need
// Name, Kind, Events, Weight, or LastRun must reach the v1 type, and this
// helper centralises that type-assertion ladder.
func V1Hooks(hooks []release.Hook) iter.Seq[*v1release.Hook] {
	return func(yield func(*v1release.Hook) bool) {
		for _, h := range hooks {
			v1h, ok := h.(*v1release.Hook)
			if !ok {
				v, cast := h.(v1release.Hook)
				if !cast {
					continue
				}
				v1h = &v
			}
			if !yield(v1h) {
				return
			}
		}
	}
}
