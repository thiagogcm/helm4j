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
// Fields are populated based on the requested mode. The "metadata" mode uses a
// separate inline shape (see getMetadata) that flattens MetadataEntry at root.
type Response struct {
	Mode     string                   `json:"mode"`
	Release  *releaseutil.ReleaseInfo `json:"release,omitempty"`
	Values   map[string]any           `json:"values,omitempty"`
	Manifest string                   `json:"manifest,omitempty"`
	Hooks    []HookEntry              `json:"hooks,omitempty"`
	Notes    string                   `json:"notes,omitempty"`
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

	env, err := helmenv.NewWithNamespace(opts.Namespace)
	if err != nil {
		return "", fmt.Errorf("bootstrap helm: %w", err)
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

// fetchAccessor creates a NewGet action, executes it, and returns both the
// raw release and its Accessor.
func fetchAccessor(env *helmenv.Env, releaseName string, opts Options, mode string) (release.Releaser, release.Accessor, error) {
	client := action.NewGet(env.Config)
	client.Version = opts.Revision

	rel, err := client.Run(releaseName)
	if err != nil {
		return nil, nil, fmt.Errorf("helm get %s: %w", mode, err)
	}

	acc, err := release.NewAccessor(rel)
	if err != nil {
		return nil, nil, fmt.Errorf("create accessor: %w", err)
	}

	return rel, acc, nil
}

func getAll(env *helmenv.Env, releaseName string, opts Options) (string, error) {
	rel, acc, err := fetchAccessor(env, releaseName, opts, "all")
	if err != nil {
		return "", err
	}

	info, err := releaseutil.MapRelease(rel)
	if err != nil {
		return "", fmt.Errorf("map release: %w", err)
	}

	resp := Response{
		Mode:     "all",
		Release:  &info,
		Manifest: acc.Manifest(),
		Notes:    acc.Notes(),
		Hooks:    mapHooks(acc.Hooks()),
	}

	// Values (config) are only available on the v1 release type.
	if v1, ok := rel.(*v1release.Release); ok {
		resp.Values = v1.Config
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
	_, acc, err := fetchAccessor(env, releaseName, opts, "manifest")
	if err != nil {
		return "", err
	}

	resp := Response{Mode: "manifest", Manifest: acc.Manifest()}
	return bridge.MarshalJSON(resp)
}

func getHooks(env *helmenv.Env, releaseName string, opts Options) (string, error) {
	_, acc, err := fetchAccessor(env, releaseName, opts, "hooks")
	if err != nil {
		return "", err
	}

	hooks := mapHooks(acc.Hooks())
	if hooks == nil {
		hooks = []HookEntry{}
	}

	resp := Response{Mode: "hooks", Hooks: hooks}
	return bridge.MarshalJSON(resp)
}

func getNotes(env *helmenv.Env, releaseName string, opts Options) (string, error) {
	_, acc, err := fetchAccessor(env, releaseName, opts, "notes")
	if err != nil {
		return "", err
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

	// Flatten metadata at root so the Java GetMetadataPayload reads fields at the
	// top level, like every other get mode (values, manifest, hooks, notes).
	resp := struct {
		Mode string `json:"mode"`
		MetadataEntry
	}{Mode: "metadata", MetadataEntry: entry}
	return bridge.MarshalJSON(resp)
}

// mapHooks converts the accessor's hook slice into the serialisable HookEntry
// type. Hook fields beyond Path and Manifest (Name, Kind, Events, Weight) require
// the v1 type, sourced via [releaseutil.V1Hooks].
func mapHooks(hooks []release.Hook) []HookEntry {
	var entries []HookEntry
	for h := range releaseutil.V1Hooks(hooks) {
		events := make([]string, len(h.Events))
		for i, e := range h.Events {
			events[i] = string(e)
		}
		entries = append(entries, HookEntry{
			Name:   h.Name,
			Kind:   h.Kind,
			Path:   h.Path,
			Events: events,
			Weight: h.Weight,
		})
	}
	return entries
}
