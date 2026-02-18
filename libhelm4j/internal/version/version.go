// Package version exposes the Helm SDK and Go runtime version metadata
// so the Java side can report accurate toolchain information.
package version

import (
	"runtime"
	"runtime/debug"

	"github.com/thiagogcm/libhelm4j/internal/bridge"
	"github.com/thiagogcm/libhelm4j/internal/helmlog"
)

// Info is the structured version payload returned across the FFM boundary.
type Info struct {
	Version     string `json:"version"`
	GoVersion   string `json:"goVersion"`
	HelmVersion string `json:"helmVersion"`
}

// Run returns the JSON-encoded version info string.
func Run() (string, error) {
	helmlog.Logger().Debug("running helm version")

	info := Info{
		GoVersion: runtime.Version(),
	}

	if buildInfo, ok := debug.ReadBuildInfo(); ok {
		info.Version = buildInfo.Main.Version
		for _, dep := range buildInfo.Deps {
			if dep.Path == "helm.sh/helm/v4" {
				info.HelmVersion = dep.Version
				break
			}
		}
	}

	return bridge.MarshalJSON(info)
}
