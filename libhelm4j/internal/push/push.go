// Package push implements `helm push` — uploading a chart to an OCI registry.
package push

import (
	"fmt"
	"log/slog"

	"helm.sh/helm/v4/pkg/action"

	"github.com/thiagogcm/libhelm4j/internal/bridge"
	"github.com/thiagogcm/libhelm4j/internal/helmenv"
	"github.com/thiagogcm/libhelm4j/internal/helmlog"
)

// Options captures the Helm flags relevant to `helm push`.
type Options struct {
	PlainHTTP             bool   `json:"plainHttp,omitempty"`
	InsecureSkipTLSVerify bool   `json:"insecureSkipTlsVerify,omitempty"`
	CertFile              string `json:"certFile,omitempty"`
	KeyFile               string `json:"keyFile,omitempty"`
	CaFile                string `json:"caFile,omitempty"`
}

// Response is the top-level JSON payload returned across the FFI boundary.
type Response struct {
	Output string `json:"output"`
}

// Run executes a helm push of chartRef to remote and returns the JSON-encoded
// response string or an error.
func Run(chartRef, remote string, opts Options) (string, error) {
	log := helmlog.Logger().With(
		slog.String("operation", "push"),
		slog.String("chartRef", chartRef),
		slog.String("remote", remote),
	)

	if chartRef == "" {
		return "", fmt.Errorf("chart reference is required")
	}
	if remote == "" {
		return "", fmt.Errorf("remote registry URL is required")
	}

	log.Debug("running helm push")

	env, err := helmenv.New()
	if err != nil {
		return "", fmt.Errorf("bootstrap helm: %w", err)
	}

	pushOpts := []action.PushOpt{
		action.WithPushConfig(env.Config),
		action.WithPlainHTTP(opts.PlainHTTP),
		action.WithInsecureSkipTLSVerify(opts.InsecureSkipTLSVerify),
	}
	if opts.CertFile != "" || opts.KeyFile != "" || opts.CaFile != "" {
		pushOpts = append(pushOpts, action.WithTLSClientConfig(opts.CertFile, opts.KeyFile, opts.CaFile))
	}

	client := action.NewPushWithOpts(pushOpts...)
	client.Settings = env.Settings

	output, err := client.Run(chartRef, remote)
	if err != nil {
		return "", fmt.Errorf("helm push: %w", err)
	}

	resp := Response{Output: output}
	result, err := bridge.MarshalJSON(resp)
	if err != nil {
		return "", err
	}

	log.Debug("helm push completed successfully")
	return result, nil
}
