// Package getrelease implements `helm get` — retrieving information about
// a named release. It dispatches to the appropriate Helm SDK action based
// on the requested mode (all, values, manifest, hooks, notes, metadata).
package getrelease

import (
	"errors"
	"fmt"
	"log/slog"
	"strings"

	"helm.sh/helm/v4/pkg/action"
	"helm.sh/helm/v4/pkg/release"
	v1release "helm.sh/helm/v4/pkg/release/v1"

	"github.com/thiagogcm/libhelm4j/internal/bridge"
	"github.com/thiagogcm/libhelm4j/internal/helmenv"
	"github.com/thiagogcm/libhelm4j/internal/helmlog"
	"github.com/thiagogcm/libhelm4j/internal/releaseutil"
)

// Options captures the Helm flags relevant to `helm get`.
type Options struct {
	Namespace string `json:"namespace,omitempty"`
	Revision  int    `json:"revision,omitempty"`
	AllValues bool   `json:"allValues,omitempty"`
}

// HookEntry represents a single Helm hook returned in the hooks response.
type HookEntry struct {
	Name   string   `json:"name"`
	Kind   string   `json:"kind"`
	Path   string   `json:"path"`
	Events []string `json:"events"`
	Weight int      `json:"weight"`
}

// MetadataEntry is the structured metadata for a release.
type MetadataEntry struct {
	Name         string `json:"name"`
	Namespace    string `json:"namespace"`
	Revision     int    `json:"revision"`
	Status       string `json:"status"`
	Chart        string `json:"chart"`
	ChartVersion string `json:"chartVersion"`
	AppVersion   string `json:"appVersion"`
	DeployedAt   string `json:"deployedAt"`
}

// Response is the top-level JSON payload returned across the FFM boundary.
// Fields are populated based on the requested mode.
type Response struct {
	Mode     string                   `json:"mode"`
	Release  *releaseutil.ReleaseInfo `json:"release,omitempty"`
	Values   map[string]any           `json:"values,omitempty"`
	Manifest string                   `json:"manifest,omitempty"`
	Hooks    []HookEntry              `json:"hooks,omitempty"`
	Notes    string                   `json:"notes,omitempty"`
	Metadata *MetadataEntry           `json:"metadata,omitempty"`
}

// Run executes a helm get operation for the given mode and release name.
func Run(mode, releaseName string, opts Options) (string, error) {
	log := helmlog.Logger().With(
		slog.String("operation", "get"),
		slog.String("mode", mode),
		slog.String("releaseName", releaseName),
	)

	if strings.TrimSpace(releaseName) == "" {
		return "", errors.New("release name is required")
	}

	log.Debug("running helm get")

	env, err := helmenv.New()
	if err != nil {
		return "", fmt.Errorf("bootstrap helm: %w", err)
	}

	if opts.Namespace != "" {
		env.Settings.SetNamespace(opts.Namespace)
	}

	switch strings.ToLower(strings.TrimSpace(mode)) {
	case "all":
		return getAll(env, releaseName, opts)
	case "values":
		return getValues(env, releaseName, opts)
	case "manifest":
		return getManifest(env, releaseName, opts)
	case "hooks":
		return getHooks(env, releaseName, opts)
	case "notes":
		return getNotes(env, releaseName, opts)
	case "metadata":
		return getMetadata(env, releaseName, opts)
	case "":
		return "", errors.New("get mode is required")
	default:
		return "", fmt.Errorf("unsupported get mode: %s", mode)
	}
}

func getAll(env *helmenv.Env, releaseName string, opts Options) (string, error) {
	client := action.NewGet(env.Config)
	client.Version = opts.Revision

	rel, err := client.Run(releaseName)
	if err != nil {
		return "", fmt.Errorf("helm get all: %w", err)
	}

	info, err := releaseutil.MapRelease(rel)
	if err != nil {
		return "", fmt.Errorf("map release: %w", err)
	}

	acc, err := release.NewAccessor(rel)
	if err != nil {
		return "", fmt.Errorf("create accessor: %w", err)
	}

	resp := Response{
		Mode:     "all",
		Release:  &info,
		Manifest: acc.Manifest(),
		Notes:    acc.Notes(),
	}

	if v1, ok := rel.(*v1release.Release); ok {
		resp.Values = v1.Config
		resp.Hooks = mapHooks(v1)
	}

	return bridge.MarshalJSON(resp)
}

func getValues(env *helmenv.Env, releaseName string, opts Options) (string, error) {
	client := action.NewGetValues(env.Config)
	client.Version = opts.Revision
	client.AllValues = opts.AllValues

	vals, err := client.Run(releaseName)
	if err != nil {
		return "", fmt.Errorf("helm get values: %w", err)
	}

	resp := Response{Mode: "values", Values: vals}
	return bridge.MarshalJSON(resp)
}

func getManifest(env *helmenv.Env, releaseName string, opts Options) (string, error) {
	client := action.NewGet(env.Config)
	client.Version = opts.Revision

	rel, err := client.Run(releaseName)
	if err != nil {
		return "", fmt.Errorf("helm get manifest: %w", err)
	}

	acc, err := release.NewAccessor(rel)
	if err != nil {
		return "", fmt.Errorf("create accessor: %w", err)
	}

	resp := Response{Mode: "manifest", Manifest: acc.Manifest()}
	return bridge.MarshalJSON(resp)
}

func getHooks(env *helmenv.Env, releaseName string, opts Options) (string, error) {
	client := action.NewGet(env.Config)
	client.Version = opts.Revision

	rel, err := client.Run(releaseName)
	if err != nil {
		return "", fmt.Errorf("helm get hooks: %w", err)
	}

	var hooks []HookEntry
	if v1, ok := rel.(*v1release.Release); ok {
		hooks = mapHooks(v1)
	}
	if hooks == nil {
		hooks = []HookEntry{}
	}

	resp := Response{Mode: "hooks", Hooks: hooks}
	return bridge.MarshalJSON(resp)
}

func getNotes(env *helmenv.Env, releaseName string, opts Options) (string, error) {
	client := action.NewGet(env.Config)
	client.Version = opts.Revision

	rel, err := client.Run(releaseName)
	if err != nil {
		return "", fmt.Errorf("helm get notes: %w", err)
	}

	acc, err := release.NewAccessor(rel)
	if err != nil {
		return "", fmt.Errorf("create accessor: %w", err)
	}

	resp := Response{Mode: "notes", Notes: acc.Notes()}
	return bridge.MarshalJSON(resp)
}

func getMetadata(env *helmenv.Env, releaseName string, opts Options) (string, error) {
	client := action.NewGetMetadata(env.Config)
	client.Version = opts.Revision

	meta, err := client.Run(releaseName)
	if err != nil {
		return "", fmt.Errorf("helm get metadata: %w", err)
	}

	entry := MetadataEntry{
		Name:         meta.Name,
		Namespace:    meta.Namespace,
		Revision:     meta.Revision,
		Status:       meta.Status,
		Chart:        meta.Chart,
		ChartVersion: meta.Version,
		AppVersion:   meta.AppVersion,
		DeployedAt:   meta.DeployedAt,
	}

	resp := Response{Mode: "metadata", Metadata: &entry}
	return bridge.MarshalJSON(resp)
}

func mapHooks(v1 *v1release.Release) []HookEntry {
	var hooks []HookEntry
	for _, hook := range v1.Hooks {
		var events []string
		for _, e := range hook.Events {
			events = append(events, string(e))
		}
		hooks = append(hooks, HookEntry{
			Name:   hook.Name,
			Kind:   hook.Kind,
			Path:   hook.Path,
			Events: events,
			Weight: hook.Weight,
		})
	}
	return hooks
}
