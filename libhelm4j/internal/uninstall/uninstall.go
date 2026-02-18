// Package uninstall implements `helm uninstall` — removing a release from
// a Kubernetes cluster.
package uninstall

import (
	"errors"
	"fmt"
	"log/slog"
	"strings"
	"time"

	"helm.sh/helm/v4/pkg/action"
	"helm.sh/helm/v4/pkg/kube"

	"github.com/thiagogcm/libhelm4j/internal/bridge"
	"github.com/thiagogcm/libhelm4j/internal/helmenv"
	"github.com/thiagogcm/libhelm4j/internal/helmlog"
	"github.com/thiagogcm/libhelm4j/internal/releaseutil"
)

// Options captures the Helm flags relevant to `helm uninstall`.
type Options struct {
	Namespace           string `json:"namespace,omitempty"`
	DryRun              bool   `json:"dryRun,omitempty"`
	DisableHooks        bool   `json:"disableHooks,omitempty"`
	KeepHistory         bool   `json:"keepHistory,omitempty"`
	IgnoreNotFound      bool   `json:"ignoreNotFound,omitempty"`
	Timeout             string `json:"timeout,omitempty"`
	Description         string `json:"description,omitempty"`
	Wait                string `json:"wait,omitempty"`
	DeletionPropagation string `json:"deletionPropagation,omitempty"`
}

// Response is the top-level JSON payload returned across the FFM boundary.
type Response struct {
	Release releaseutil.ReleaseInfo `json:"release"`
	Info    string                  `json:"info"`
}

// Run executes a helm uninstall operation for the given release name.
func Run(releaseName string, opts Options) (string, error) {
	log := helmlog.Logger().With(
		slog.String("operation", "uninstall"),
		slog.String("releaseName", releaseName),
	)

	if strings.TrimSpace(releaseName) == "" {
		return "", errors.New("release name is required")
	}
	if opts.Timeout != "" {
		_, err := time.ParseDuration(opts.Timeout)
		if err != nil {
			return "", fmt.Errorf("invalid timeout %q: %w", opts.Timeout, err)
		}
	}

	log.Debug("running helm uninstall")

	env, err := helmenv.NewWithNamespace(opts.Namespace)
	if err != nil {
		return "", fmt.Errorf("bootstrap helm: %w", err)
	}

	client := action.NewUninstall(env.Config)
	client.DryRun = opts.DryRun
	client.DisableHooks = opts.DisableHooks
	client.KeepHistory = opts.KeepHistory
	client.IgnoreNotFound = opts.IgnoreNotFound
	client.Description = opts.Description
	client.DeletionPropagation = opts.DeletionPropagation

	if opts.Wait != "" {
		client.WaitStrategy = kube.WaitStrategy(opts.Wait)
	}
	if opts.Timeout != "" {
		d, _ := time.ParseDuration(opts.Timeout) // already validated above in Run
		client.Timeout = d
	}

	res, err := client.Run(releaseName)
	if err != nil {
		return "", fmt.Errorf("helm uninstall: %w", err)
	}

	resp := Response{Info: res.Info}
	if res.Release != nil {
		info, mapErr := releaseutil.MapRelease(res.Release)
		if mapErr != nil {
			return "", fmt.Errorf("map release: %w", mapErr)
		}
		resp.Release = info
	}

	result, err := bridge.MarshalJSON(resp)
	if err != nil {
		return "", err
	}

	log.Debug("helm uninstall completed successfully")
	return result, nil
}
