// Package pkg implements `helm package` — packaging a chart directory into a .tgz archive.
package pkg

import (
	"fmt"
	"log/slog"

	"helm.sh/helm/v4/pkg/action"

	"github.com/thiagogcm/libhelm4j/internal/bridge"
	"github.com/thiagogcm/libhelm4j/internal/helmlog"
)

// Options captures the Helm flags relevant to `helm package`.
type Options struct {
	Version               string `json:"version,omitempty"`
	AppVersion            string `json:"appVersion,omitempty"`
	Destination           string `json:"destination,omitempty"`
	DependencyUpdate      bool   `json:"dependencyUpdate,omitempty"`
	Sign                  bool   `json:"sign,omitempty"`
	Key                   string `json:"key,omitempty"`
	Keyring               string `json:"keyring,omitempty"`
	PassphraseFile        string `json:"passphraseFile,omitempty"`
	PlainHTTP             bool   `json:"plainHttp,omitempty"`
	InsecureSkipTLSVerify bool   `json:"insecureSkipTlsVerify,omitempty"`
	CertFile              string `json:"certFile,omitempty"`
	KeyFile               string `json:"keyFile,omitempty"`
	CaFile                string `json:"caFile,omitempty"`
}

// Response is the top-level JSON payload returned across the FFM boundary.
type Response struct {
	Path string `json:"path"`
}

// Run executes a helm package operation for the given chart path.
func Run(chartPath string, opts Options) (string, error) {
	log := helmlog.Logger().With(
		slog.String("operation", "package"),
		slog.String("chartPath", chartPath),
	)

	if chartPath == "" {
		return "", fmt.Errorf("chart path is required")
	}

	log.Debug("running helm package")

	client := action.NewPackage()
	client.Version = opts.Version
	client.AppVersion = opts.AppVersion
	client.Destination = opts.Destination
	client.DependencyUpdate = opts.DependencyUpdate
	client.Sign = opts.Sign
	client.Key = opts.Key
	client.Keyring = opts.Keyring
	client.PassphraseFile = opts.PassphraseFile
	client.PlainHTTP = opts.PlainHTTP
	client.InsecureSkipTLSVerify = opts.InsecureSkipTLSVerify
	client.CertFile = opts.CertFile
	client.KeyFile = opts.KeyFile
	client.CaFile = opts.CaFile

	path, err := client.Run(chartPath, nil)
	if err != nil {
		return "", fmt.Errorf("helm package: %w", err)
	}

	resp := Response{Path: path}
	result, err := bridge.MarshalJSON(resp)
	if err != nil {
		return "", err
	}

	log.Debug("helm package completed successfully", slog.String("path", path))
	return result, nil
}
