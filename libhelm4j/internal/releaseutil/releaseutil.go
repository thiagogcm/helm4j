// Package releaseutil provides shared release mapping utilities used by
// operations that return release information across the FFM boundary.
package releaseutil

import (
	"fmt"
	"time"

	chart "helm.sh/helm/v4/pkg/chart/v2"
	"helm.sh/helm/v4/pkg/release"
	v1release "helm.sh/helm/v4/pkg/release/v1"
)

// ReleaseInfo is the structured release payload returned on success.
type ReleaseInfo struct {
	Name          string `json:"name"`
	Namespace     string `json:"namespace"`
	Revision      int    `json:"revision"`
	Status        string `json:"status"`
	Description   string `json:"description"`
	FirstDeployed string `json:"firstDeployed"`
	LastDeployed  string `json:"lastDeployed"`
	ChartName     string `json:"chartName"`
	ChartVersion  string `json:"chartVersion"`
	AppVersion    string `json:"appVersion"`
	Notes         string `json:"notes"`
}

// MapRelease converts a Helm SDK [release.Releaser] into the serialisable
// [ReleaseInfo] returned across the FFM boundary. It uses the [release.Accessor]
// interface for forward-compatible field access, falling back to a type
// assertion for fields only available on the concrete v1 release type.
func MapRelease(rel release.Releaser) (ReleaseInfo, error) {
	acc, err := release.NewAccessor(rel)
	if err != nil {
		return ReleaseInfo{}, fmt.Errorf("create release accessor: %w", err)
	}

	info := ReleaseInfo{
		Name:      acc.Name(),
		Namespace: acc.Namespace(),
		Revision:  acc.Version(),
		Status:    acc.Status(),
		Notes:     acc.Notes(),
	}

	if ch := acc.Chart(); ch != nil {
		if v2ch, ok := ch.(*chart.Chart); ok && v2ch.Metadata != nil {
			info.ChartName = v2ch.Metadata.Name
			info.ChartVersion = v2ch.Metadata.Version
			info.AppVersion = v2ch.Metadata.AppVersion
		}
	}

	if v1, ok := rel.(*v1release.Release); ok && v1.Info != nil {
		info.Description = v1.Info.Description
		if !v1.Info.FirstDeployed.IsZero() {
			info.FirstDeployed = v1.Info.FirstDeployed.UTC().Format(time.RFC3339)
		}
		if !v1.Info.LastDeployed.IsZero() {
			info.LastDeployed = v1.Info.LastDeployed.UTC().Format(time.RFC3339)
		}
	}

	return info, nil
}
