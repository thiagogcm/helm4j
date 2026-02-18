// Package history implements `helm history` — viewing the revision history
// of a release.
package history

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

// Options captures the Helm flags relevant to `helm history`.
type Options struct {
	Namespace string `json:"namespace,omitempty"`
	Max       int    `json:"max,omitempty"`
}

// Entry represents one revision in the release history.
type Entry struct {
	Revision     int    `json:"revision"`
	Updated      string `json:"updated"`
	Status       string `json:"status"`
	Chart        string `json:"chart"`
	ChartVersion string `json:"chartVersion"`
	AppVersion   string `json:"appVersion"`
	Description  string `json:"description"`
}

// Response is the top-level JSON payload returned across the FFM boundary.
type Response struct {
	Entries []Entry `json:"entries"`
}

// Run executes a helm history operation for the given release name.
func Run(releaseName string, opts Options) (string, error) {
	log := helmlog.Logger().With(
		slog.String("operation", "history"),
		slog.String("releaseName", releaseName),
	)

	if strings.TrimSpace(releaseName) == "" {
		return "", errors.New("release name is required")
	}

	log.Debug("running helm history")

	env, err := helmenv.NewWithNamespace(opts.Namespace)
	if err != nil {
		return "", fmt.Errorf("bootstrap helm: %w", err)
	}

	client := action.NewHistory(env.Config)
	client.Max = opts.Max

	releases, err := client.Run(releaseName)
	if err != nil {
		return "", fmt.Errorf("helm history: %w", err)
	}

	var entries []Entry
	for _, rel := range releases {
		info, mapErr := releaseutil.MapRelease(rel)
		if mapErr != nil {
			return "", fmt.Errorf("map release: %w", mapErr)
		}
		entries = append(entries, Entry{
			Revision:     info.Revision,
			Updated:      info.LastDeployed,
			Status:       info.Status,
			Chart:        info.ChartName,
			ChartVersion: info.ChartVersion,
			AppVersion:   info.AppVersion,
			Description:  info.Description,
		})
	}
	if entries == nil {
		entries = []Entry{}
	}

	resp := Response{Entries: entries}
	result, err := bridge.MarshalJSON(resp)
	if err != nil {
		return "", err
	}

	log.Debug("helm history completed", slog.Int("entries", len(entries)))
	return result, nil
}
