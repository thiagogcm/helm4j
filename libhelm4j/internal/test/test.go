// Package test implements `helm test` — running chart test hooks in a release.
package test

import (
	"errors"
	"fmt"
	"log/slog"
	"strings"
	"time"

	"helm.sh/helm/v4/pkg/action"
	v1release "helm.sh/helm/v4/pkg/release/v1"

	"github.com/thiagogcm/libhelm4j/internal/bridge"
	"github.com/thiagogcm/libhelm4j/internal/helmenv"
	"github.com/thiagogcm/libhelm4j/internal/helmlog"
	"github.com/thiagogcm/libhelm4j/internal/releaseutil"
)

// Options captures the Helm flags relevant to `helm test`.
type Options struct {
	Namespace string   `json:"namespace,omitempty"`
	Timeout   string   `json:"timeout,omitempty"`
	Filter    []string `json:"filter,omitempty"` // hook name filter (include by name)
}

// TestResult captures the outcome of a single test hook.
type TestResult struct {
	Name   string `json:"name"`
	Status string `json:"status"`
}

// Response is the top-level JSON payload returned across the FFM boundary.
type Response struct {
	Release releaseutil.ReleaseInfo `json:"release"`
	Results []TestResult            `json:"results"`
}

// Run executes a helm test operation for the given release name.
func Run(releaseName string, opts Options) (string, error) {
	log := helmlog.Logger().With(
		slog.String("operation", "test"),
		slog.String("releaseName", releaseName),
	)

	if strings.TrimSpace(releaseName) == "" {
		return "", errors.New("release name is required")
	}
	if opts.Timeout != "" {
		if _, err := time.ParseDuration(opts.Timeout); err != nil {
			return "", fmt.Errorf("invalid timeout %q: %w", opts.Timeout, err)
		}
	}

	log.Debug("running helm test")

	env, err := helmenv.NewWithNamespace(opts.Namespace)
	if err != nil {
		return "", fmt.Errorf("bootstrap helm: %w", err)
	}

	client := action.NewReleaseTesting(env.Config)
	client.Namespace = env.Settings.Namespace()
	if opts.Timeout != "" {
		d, _ := time.ParseDuration(opts.Timeout) // already validated above
		client.Timeout = d
	}
	if len(opts.Filter) > 0 {
		client.Filters[action.IncludeNameFilter] = opts.Filter
	}

	rel, shutdown, err := client.Run(releaseName)
	if shutdown != nil {
		defer shutdown() //nolint:errcheck
	}
	if err != nil {
		return "", fmt.Errorf("helm test: %w", err)
	}

	info, mapErr := releaseutil.MapRelease(rel)
	if mapErr != nil {
		return "", fmt.Errorf("map release: %w", mapErr)
	}

	// Collect test hook results from the v1 release type.
	var results []TestResult
	if v1, ok := rel.(*v1release.Release); ok {
		for _, h := range v1.Hooks {
			for _, e := range h.Events {
				if e == v1release.HookTest {
					results = append(results, TestResult{
						Name:   h.Name,
						Status: string(h.LastRun.Phase),
					})
					break
				}
			}
		}
	}
	if results == nil {
		results = []TestResult{}
	}

	resp := Response{Release: info, Results: results}
	result, err := bridge.MarshalJSON(resp)
	if err != nil {
		return "", err
	}

	log.Debug("helm test completed successfully", slog.Int("tests", len(results)))
	return result, nil
}
