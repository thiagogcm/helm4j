// Package dependency implements `helm dependency list` — listing chart dependencies.
package dependency

import (
	"bytes"
	"fmt"
	"log/slog"

	"helm.sh/helm/v4/pkg/action"

	"github.com/thiagogcm/libhelm4j/internal/bridge"
	"github.com/thiagogcm/libhelm4j/internal/helmlog"
)

// Options captures the Helm flags relevant to `helm dependency list`.
type Options struct {
	SkipRefresh           bool   `json:"skipRefresh,omitempty"`
	Verify                bool   `json:"verify,omitempty"`
	Keyring               string `json:"keyring,omitempty"`
	PlainHTTP             bool   `json:"plainHttp,omitempty"`
	InsecureSkipTLSVerify bool   `json:"insecureSkipTlsVerify,omitempty"`
	CertFile              string `json:"certFile,omitempty"`
	KeyFile               string `json:"keyFile,omitempty"`
	CaFile                string `json:"caFile,omitempty"`
}

// Response is the top-level JSON payload returned across the FFM boundary.
type Response struct {
	Output string `json:"output"`
}

// Run executes a helm dependency list operation for the given chart path.
func Run(chartPath string, opts Options) (string, error) {
	log := helmlog.Logger().With(
		slog.String("operation", "dependency"),
		slog.String("chartPath", chartPath),
	)

	if chartPath == "" {
		return "", fmt.Errorf("chart path is required")
	}

	log.Debug("running helm dependency list")

	client := action.NewDependency()
	client.SkipRefresh = opts.SkipRefresh
	client.Verify = opts.Verify
	client.Keyring = opts.Keyring
	client.PlainHTTP = opts.PlainHTTP
	client.InsecureSkipTLSVerify = opts.InsecureSkipTLSVerify
	client.CertFile = opts.CertFile
	client.KeyFile = opts.KeyFile
	client.CaFile = opts.CaFile

	var buf bytes.Buffer
	if err := client.List(chartPath, &buf); err != nil {
		return "", fmt.Errorf("helm dependency list: %w", err)
	}

	resp := Response{Output: buf.String()}
	result, err := bridge.MarshalJSON(resp)
	if err != nil {
		return "", err
	}

	log.Debug("helm dependency list completed successfully")
	return result, nil
}
