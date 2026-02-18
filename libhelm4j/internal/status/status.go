// Package status implements `helm status` — checking the deployment status
// of a release.
package status

import (
	"errors"
	"fmt"
	"log/slog"
	"strings"

	"helm.sh/helm/v4/pkg/action"

	"github.com/thiagogcm/libhelm4j/internal/bridge"
	"github.com/thiagogcm/libhelm4j/internal/helmenv"
	"github.com/thiagogcm/libhelm4j/internal/helmlog"
	"github.com/thiagogcm/libhelm4j/internal/releaseutil"
)

// Options captures the Helm flags relevant to `helm status`.
type Options struct {
	Namespace string `json:"namespace,omitempty"`
	Revision  int    `json:"revision,omitempty"`
}

// Response is the top-level JSON payload returned across the FFM boundary.
type Response struct {
	Release releaseutil.ReleaseInfo `json:"release"`
}

// Run executes a helm status operation for the given release name.
func Run(releaseName string, opts Options) (string, error) {
	log := helmlog.Logger().With(
		slog.String("operation", "status"),
		slog.String("releaseName", releaseName),
	)

	if strings.TrimSpace(releaseName) == "" {
		return "", errors.New("release name is required")
	}

	log.Debug("running helm status")

	env, err := helmenv.NewWithNamespace(opts.Namespace)
	if err != nil {
		return "", fmt.Errorf("bootstrap helm: %w", err)
	}

	client := action.NewStatus(env.Config)
	client.Version = opts.Revision

	rel, err := client.Run(releaseName)
	if err != nil {
		return "", fmt.Errorf("helm status: %w", err)
	}

	info, err := releaseutil.MapRelease(rel)
	if err != nil {
		return "", fmt.Errorf("map release: %w", err)
	}

	resp := Response{Release: info}
	result, err := bridge.MarshalJSON(resp)
	if err != nil {
		return "", err
	}

	log.Debug("helm status completed successfully")
	return result, nil
}
