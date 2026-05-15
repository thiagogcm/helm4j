// Package pull implements `helm pull` — downloading a chart to local disk.
package pull

import (
	"fmt"
	"log/slog"

	"helm.sh/helm/v4/pkg/action"

	"github.com/thiagogcm/libhelm4j/internal/bridge"
	"github.com/thiagogcm/libhelm4j/internal/helmenv"
	"github.com/thiagogcm/libhelm4j/internal/helmlog"
)

// Options captures the Helm flags relevant to `helm pull`.
type Options struct {
	// Chart resolution (shared with install, upgrade, template, show)
	helmenv.ChartPathOpts

	// Pull behaviour
	Untar    bool   `json:"untar,omitempty"`
	UntarDir string `json:"untarDir,omitempty"`
	DestDir  string `json:"destDir,omitempty"`
}

// Response is the top-level JSON payload returned across the FFM boundary.
type Response struct {
	Output string `json:"output"`
}

// Run executes a helm pull for the given chart reference and returns the
// JSON-encoded response string or an error.
func Run(chartRef string, opts Options) (string, error) {
	log := helmlog.Logger().With(
		slog.String("operation", "pull"),
		slog.String("chartRef", chartRef),
	)

	if chartRef == "" {
		return "", fmt.Errorf("chart reference is required")
	}

	log.Debug("running helm pull")

	env, err := helmenv.New()
	if err != nil {
		return "", fmt.Errorf("bootstrap helm: %w", err)
	}

	regClient, err := helmenv.BuildRegistryClient(env.Settings, opts.ChartPathOpts.RegistryOptions())
	if err != nil {
		return "", fmt.Errorf("registry client: %w", err)
	}

	client := action.NewPull(action.WithConfig(env.Config))
	client.Settings = env.Settings
	client.SetRegistryClient(regClient)

	opts.ChartPathOpts.ApplyTo(&client.ChartPathOptions)
	client.Devel = opts.Devel

	// Pull-specific
	client.Untar = opts.Untar
	if opts.UntarDir != "" {
		client.UntarDir = opts.UntarDir
	}
	if opts.DestDir != "" {
		client.DestDir = opts.DestDir
	}

	output, err := client.Run(chartRef)
	if err != nil {
		return "", fmt.Errorf("helm pull: %w", err)
	}

	resp := Response{Output: output}
	result, err := bridge.MarshalJSON(resp)
	if err != nil {
		return "", err
	}

	log.Debug("helm pull completed successfully")
	return result, nil
}
