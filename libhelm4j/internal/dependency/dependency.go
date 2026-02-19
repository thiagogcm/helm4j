// Package dependency implements `helm dependency` — managing chart dependencies.
// Supported modes: list, build, update.
package dependency

import (
	"bytes"
	"fmt"
	"log/slog"

	"helm.sh/helm/v4/pkg/action"
	"helm.sh/helm/v4/pkg/downloader"
	"helm.sh/helm/v4/pkg/getter"

	"github.com/thiagogcm/libhelm4j/internal/bridge"
	"github.com/thiagogcm/libhelm4j/internal/helmenv"
	"github.com/thiagogcm/libhelm4j/internal/helmlog"
)

// Options captures the Helm flags relevant to `helm dependency`.
type Options struct {
	// Mode selects the sub-operation: "list" (default), "build", or "update".
	Mode                  string `json:"mode,omitempty"`
	SkipRefresh           bool   `json:"skipRefresh,omitempty"`
	Verify                bool   `json:"verify,omitempty"`
	Keyring               string `json:"keyring,omitempty"`
	PlainHTTP             bool   `json:"plainHttp,omitempty"`
	InsecureSkipTLSVerify bool   `json:"insecureSkipTlsVerify,omitempty"`
	CertFile              string `json:"certFile,omitempty"`
	KeyFile               string `json:"keyFile,omitempty"`
	CaFile                string `json:"caFile,omitempty"`
	Username              string `json:"username,omitempty"`
	Password              string `json:"password,omitempty"`
}

// Response is the top-level JSON payload returned across the FFM boundary.
type Response struct {
	Output string `json:"output"`
}

// Run executes a helm dependency operation for the given chart path.
// The mode is read from opts.Mode; when empty, "list" is assumed.
func Run(chartPath string, opts Options) (string, error) {
	mode := opts.Mode
	if mode == "" {
		mode = "list"
	}

	log := helmlog.Logger().With(
		slog.String("operation", "dependency"),
		slog.String("mode", mode),
		slog.String("chartPath", chartPath),
	)

	if chartPath == "" {
		return "", fmt.Errorf("chart path is required")
	}

	switch mode {
	case "list":
		return runList(chartPath, opts, log)
	case "build":
		return runBuildOrUpdate(chartPath, opts, false, log)
	case "update":
		return runBuildOrUpdate(chartPath, opts, true, log)
	default:
		return "", fmt.Errorf("unsupported dependency mode: %s", mode)
	}
}

func runList(chartPath string, opts Options, log *slog.Logger) (string, error) {
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

func runBuildOrUpdate(chartPath string, opts Options, update bool, log *slog.Logger) (string, error) {
	op := "build"
	if update {
		op = "update"
	}
	log.Debug("running helm dependency " + op)

	env, err := helmenv.New()
	if err != nil {
		return "", fmt.Errorf("bootstrap helm: %w", err)
	}

	regClient, err := helmenv.BuildRegistryClient(env.Settings, helmenv.RegistryOptions{
		CertFile:              opts.CertFile,
		KeyFile:               opts.KeyFile,
		CaFile:                opts.CaFile,
		InsecureSkipTLSVerify: opts.InsecureSkipTLSVerify,
		PlainHTTP:             opts.PlainHTTP,
		Username:              opts.Username,
		Password:              opts.Password,
	})
	if err != nil {
		return "", fmt.Errorf("registry client: %w", err)
	}

	var buf bytes.Buffer
	man := &downloader.Manager{
		Out:              &buf,
		ChartPath:        chartPath,
		Keyring:          opts.Keyring,
		SkipUpdate:       opts.SkipRefresh,
		Getters:          getter.Getters(),
		RegistryClient:   regClient,
		RepositoryConfig: env.Settings.RepositoryConfig,
		RepositoryCache:  env.Settings.RepositoryCache,
		ContentCache:     env.Settings.ContentCache,
		Debug:            env.Settings.Debug,
	}

	if opts.Verify {
		if update {
			man.Verify = downloader.VerifyAlways
		} else {
			man.Verify = downloader.VerifyIfPossible
		}
	}

	if update {
		err = man.Update()
	} else {
		err = man.Build()
	}
	if err != nil {
		return "", fmt.Errorf("helm dependency %s: %w", op, err)
	}

	resp := Response{Output: buf.String()}
	result, err := bridge.MarshalJSON(resp)
	if err != nil {
		return "", err
	}

	log.Debug("helm dependency " + op + " completed successfully")
	return result, nil
}
