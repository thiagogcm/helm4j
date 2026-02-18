// Package list implements `helm list` — listing releases in a Kubernetes cluster.
package list

import (
	"fmt"
	"log/slog"

	"helm.sh/helm/v4/pkg/action"

	"github.com/thiagogcm/libhelm4j/internal/bridge"
	"github.com/thiagogcm/libhelm4j/internal/helmenv"
	"github.com/thiagogcm/libhelm4j/internal/helmlog"
	"github.com/thiagogcm/libhelm4j/internal/releaseutil"
)

// Options captures the Helm flags relevant to `helm list`.
type Options struct {
	Namespace     string   `json:"namespace,omitempty"`
	AllNamespaces bool     `json:"allNamespaces,omitempty"`
	Filter        string   `json:"filter,omitempty"`
	States        []string `json:"states,omitempty"` // e.g. ["deployed","failed"]
	Limit         int      `json:"limit,omitempty"`
	Offset        int      `json:"offset,omitempty"`
	SortByDate    bool     `json:"sortByDate,omitempty"`
	SortReverse   bool     `json:"sortReverse,omitempty"`
	Selector      string   `json:"selector,omitempty"` // label selector
}

// Response is the top-level JSON payload returned across the FFM boundary.
type Response struct {
	Releases []releaseutil.ReleaseInfo `json:"releases"`
}

// Run executes a helm list operation and returns the JSON-encoded response.
func Run(opts Options) (string, error) {
	log := helmlog.Logger().With(slog.String("operation", "list"))

	log.Debug("running helm list")

	env, err := helmenv.NewWithNamespace(opts.Namespace)
	if err != nil {
		return "", fmt.Errorf("bootstrap helm: %w", err)
	}

	client := action.NewList(env.Config)
	client.AllNamespaces = opts.AllNamespaces
	client.Filter = opts.Filter
	client.Limit = opts.Limit
	client.Offset = opts.Offset
	client.ByDate = opts.SortByDate
	client.SortReverse = opts.SortReverse
	client.Selector = opts.Selector

	if len(opts.States) > 0 {
		var mask action.ListStates
		for _, s := range opts.States {
			mask |= action.ListStates(0).FromName(s)
		}
		client.StateMask = mask
	} else {
		// Default: all states (mirrors `helm list --all`).
		client.All = true
	}
	client.SetStateMask()

	releases, err := client.Run()
	if err != nil {
		return "", fmt.Errorf("helm list: %w", err)
	}

	infos := make([]releaseutil.ReleaseInfo, 0, len(releases))
	for _, rel := range releases {
		info, mapErr := releaseutil.MapRelease(rel)
		if mapErr != nil {
			return "", fmt.Errorf("map release: %w", mapErr)
		}
		infos = append(infos, info)
	}

	resp := Response{Releases: infos}
	result, err := bridge.MarshalJSON(resp)
	if err != nil {
		return "", err
	}

	log.Debug("helm list completed", slog.Int("count", len(infos)))
	return result, nil
}
