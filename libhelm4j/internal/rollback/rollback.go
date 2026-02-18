// Package rollback implements `helm rollback` — rolling back a release
// to a previous revision.
package rollback

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
)

// Options captures the Helm flags relevant to `helm rollback`.
type Options struct {
	Namespace       string `json:"namespace,omitempty"`
	Revision        int    `json:"revision,omitempty"`
	DryRun          string `json:"dryRun,omitempty"`
	Timeout         string `json:"timeout,omitempty"`
	Wait            string `json:"wait,omitempty"`
	WaitForJobs     bool   `json:"waitForJobs,omitempty"`
	DisableHooks    bool   `json:"disableHooks,omitempty"`
	ForceReplace    bool   `json:"forceReplace,omitempty"`
	ForceConflicts  bool   `json:"forceConflicts,omitempty"`
	ServerSideApply *bool  `json:"serverSideApply,omitempty"`
	CleanupOnFail   bool   `json:"cleanupOnFail,omitempty"`
	MaxHistory      int    `json:"maxHistory,omitempty"`
}

// Response is the top-level JSON payload returned across the FFM boundary.
type Response struct {
	ReleaseName string `json:"releaseName"`
	Revision    int    `json:"revision"`
}

// Run executes a helm rollback operation for the given release name.
func Run(releaseName string, opts Options) (string, error) {
	log := helmlog.Logger().With(
		slog.String("operation", "rollback"),
		slog.String("releaseName", releaseName),
	)

	if strings.TrimSpace(releaseName) == "" {
		return "", errors.New("release name is required")
	}

	log.Debug("running helm rollback")

	env, err := helmenv.New()
	if err != nil {
		return "", fmt.Errorf("bootstrap helm: %w", err)
	}

	if opts.Namespace != "" {
		env.Settings.SetNamespace(opts.Namespace)
	}

	client := action.NewRollback(env.Config)
	client.Version = opts.Revision
	client.DisableHooks = opts.DisableHooks
	client.ForceReplace = opts.ForceReplace
	client.ForceConflicts = opts.ForceConflicts
	client.CleanupOnFail = opts.CleanupOnFail
	client.MaxHistory = opts.MaxHistory
	client.WaitForJobs = opts.WaitForJobs

	if opts.ServerSideApply != nil {
		if *opts.ServerSideApply {
			client.ServerSideApply = "true"
		} else {
			client.ServerSideApply = "false"
		}
	}
	if opts.DryRun != "" {
		client.DryRunStrategy = action.DryRunStrategy(opts.DryRun)
	}
	if opts.Wait != "" {
		client.WaitStrategy = kube.WaitStrategy(opts.Wait)
	}
	if opts.Timeout != "" {
		if d, parseErr := time.ParseDuration(opts.Timeout); parseErr == nil {
			client.Timeout = d
		}
	}

	err = client.Run(releaseName)
	if err != nil {
		return "", fmt.Errorf("helm rollback: %w", err)
	}

	resp := Response{
		ReleaseName: releaseName,
		Revision:    opts.Revision,
	}
	result, err := bridge.MarshalJSON(resp)
	if err != nil {
		return "", err
	}

	log.Debug("helm rollback completed successfully")
	return result, nil
}
