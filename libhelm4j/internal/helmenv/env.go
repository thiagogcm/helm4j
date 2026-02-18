// Package helmenv provisions and manages Helm CLI environments used by
// every native operation. It wraps [helm.sh/helm/v4/pkg/cli] and
// [helm.sh/helm/v4/pkg/action] into a single Env value that can be
// passed to operation-specific runners.
package helmenv

import (
	"fmt"
	"log/slog"
	"os"

	"helm.sh/helm/v4/pkg/action"
	chart "helm.sh/helm/v4/pkg/chart/v2"
	"helm.sh/helm/v4/pkg/chart/v2/loader"
	"helm.sh/helm/v4/pkg/cli"

	"github.com/thiagogcm/libhelm4j/internal/helmlog"
)

// Env bundles the Helm CLI settings and action configuration needed by
// every Helm operation. Create one via [New].
type Env struct {
	Settings *cli.EnvSettings
	Config   *action.Configuration
}

// Options configures environment initialization behavior.
type Options struct {
	Namespace string
}

// New provisions a fresh Helm CLI environment and action configuration.
// It mirrors the initialisation performed by the helm CLI binary so that
// every operation starts from a consistent baseline.
func New() (*Env, error) {
	return NewWithOptions(Options{})
}

// NewWithNamespace provisions an environment pinned to the provided namespace.
func NewWithNamespace(namespace string) (*Env, error) {
	return NewWithOptions(Options{Namespace: namespace})
}

// NewWithOptions provisions a fresh Helm CLI environment with explicit
// initialization options for shared bootstrap settings.
func NewWithOptions(opts Options) (*Env, error) {
	debugEnabled := helmlog.SetLevelFromEnv()

	settings := cli.New()
	settings.Debug = debugEnabled
	if opts.Namespace != "" {
		settings.SetNamespace(opts.Namespace)
	}

	cfg := action.NewConfiguration(action.ConfigurationSetLogger(helmlog.Handler()))
	if err := cfg.Init(settings.RESTClientGetter(), settings.Namespace(), os.Getenv("HELM_DRIVER")); err != nil {
		return nil, fmt.Errorf("init action configuration: %w", err)
	}

	helmlog.Logger().Debug("initialized helm environment", slog.String("namespace", settings.Namespace()))

	return &Env{Settings: settings, Config: cfg}, nil
}

// LocateAndLoadChart resolves the chart reference to a path on disk and
// loads it. If devel is true and version is empty, prerelease versions
// are included.
func LocateAndLoadChart(chartRef, version string, devel bool, settings *cli.EnvSettings, locator ChartLocator) (string, *chart.Chart, error) {
	if version == "" && devel {
		version = ">0.0.0-0"
	}

	chartPath, err := locator.LocateChart(chartRef, settings)
	if err != nil {
		return "", nil, fmt.Errorf("locate chart: %w", err)
	}

	helmlog.Logger().Debug(
		"resolved chart reference",
		slog.String("chartRef", chartRef),
		slog.String("chartPath", chartPath),
	)

	ch, err := loader.Load(chartPath)
	if err != nil {
		return "", nil, fmt.Errorf("load chart: %w", err)
	}

	return chartPath, ch, nil
}

// ChartLocator is implemented by Helm action types that can resolve a chart
// reference to a local path (e.g. [action.Show], [action.Install]).
type ChartLocator interface {
	LocateChart(name string, settings *cli.EnvSettings) (string, error)
}
