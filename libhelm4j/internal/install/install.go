// Package install implements `helm install` — installing a chart into a
// Kubernetes cluster via the Helm SDK action layer.
package install

import (
	"context"
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

// Options captures the Helm flags relevant to `helm install` and chart
// discovery. JSON tags use the same camelCase convention as every other
// operation in libhelm4j.
type Options struct {
	// Chart resolution (shared with upgrade, template, show, pull)
	helmenv.ChartPathOpts

	// Install behaviour
	Namespace                string `json:"namespace,omitempty"`
	CreateNamespace          bool   `json:"createNamespace,omitempty"`
	DryRun                   string `json:"dryRun,omitempty"`
	Wait                     string `json:"wait,omitempty"`
	WaitForJobs              bool   `json:"waitForJobs,omitempty"`
	Timeout                  string `json:"timeout,omitempty"`
	Description              string `json:"description,omitempty"`
	RollbackOnFailure        bool   `json:"rollbackOnFailure,omitempty"`
	SkipCRDs                 bool   `json:"skipCrds,omitempty"`
	DisableHooks             bool   `json:"disableHooks,omitempty"`
	DisableOpenAPIValidation bool   `json:"disableOpenApiValidation,omitempty"`
	ForceReplace             bool   `json:"forceReplace,omitempty"`
	ForceConflicts           bool   `json:"forceConflicts,omitempty"`
	ServerSideApply          *bool  `json:"serverSideApply,omitempty"`
	Replace                  bool   `json:"replace,omitempty"`
	GenerateName             bool   `json:"generateName,omitempty"`
	NameTemplate             string `json:"nameTemplate,omitempty"`
	SubNotes                 bool   `json:"subNotes,omitempty"`
	EnableDNS                bool   `json:"enableDns,omitempty"`
	TakeOwnership            bool   `json:"takeOwnership,omitempty"`
	DependencyUpdate         bool   `json:"dependencyUpdate,omitempty"`

	// Values — a pre-merged values map from the caller.
	Values map[string]any    `json:"values,omitempty"`
	Labels map[string]string `json:"labels,omitempty"`
}

// ReleaseInfo is an alias for the shared release info type.
type ReleaseInfo = releaseutil.ReleaseInfo

// Response is the top-level JSON payload returned across the FFM boundary.
type Response struct {
	Release releaseutil.ReleaseInfo `json:"release"`
}

// Run executes a helm install operation for the given release name and chart
// reference. It returns the JSON-encoded response string or an error.
func Run(releaseName, chartRef string, opts Options) (string, error) {
	log := helmlog.Logger().With(
		slog.String("operation", "install"),
		slog.String("releaseName", releaseName),
		slog.String("chartRef", chartRef),
	)

	if strings.TrimSpace(chartRef) == "" {
		return "", errors.New("chart reference is required")
	}

	if opts.Timeout != "" {
		_, err := time.ParseDuration(opts.Timeout)
		if err != nil {
			return "", fmt.Errorf("invalid timeout %q: %w", opts.Timeout, err)
		}
	}

	if err := bridge.ValidateWaitStrategy(opts.Wait); err != nil {
		return "", err
	}

	log.Debug("running helm install")

	// --- bootstrap environment ---
	env, err := helmenv.NewWithNamespace(opts.Namespace)
	if err != nil {
		log.Warn("failed to initialize helm environment", slog.Any("error", err))
		return "", fmt.Errorf("bootstrap helm: %w", err)
	}

	regClient, err := helmenv.BuildRegistryClient(env.Settings, opts.ChartPathOpts.RegistryOptions())
	if err != nil {
		log.Warn("failed to initialize registry client", slog.Any("error", err))
		return "", fmt.Errorf("registry client: %w", err)
	}
	env.Config.RegistryClient = regClient

	// --- configure the install action ---
	client := action.NewInstall(env.Config)
	client.SetRegistryClient(regClient)
	applyOptions(client, opts)

	// Use NameAndChart to handle generateName, nameTemplate, and mutual
	// exclusion validations exactly like the Helm CLI.
	name, _, err := client.NameAndChart([]string{releaseName, chartRef})
	if err != nil {
		return "", fmt.Errorf("resolve name and chart: %w", err)
	}
	client.ReleaseName = name

	// --- locate and load the chart ---
	chartPath, ch, err := helmenv.LocateAndLoadChart(chartRef, client.Version, client.Devel, env.Settings, client)
	if err != nil {
		log.Warn("failed to locate or load chart", slog.Any("error", err))
		return "", err
	}

	// --- dependency preflight ---
	ch, err = helmenv.EnsureDependencies(env, ch, chartPath, chartRef, client.Version, client.Devel, opts.DependencyUpdate, client)
	if err != nil {
		return "", err
	}

	// --- run the install ---
	vals := opts.Values
	if vals == nil {
		vals = map[string]any{}
	}

	ctx := context.Background()
	if client.Timeout > 0 {
		var cancel context.CancelFunc
		ctx, cancel = context.WithTimeout(ctx, client.Timeout)
		defer cancel()
	}

	rel, err := client.RunWithContext(ctx, ch, vals)
	if err != nil {
		log.Warn("helm install command failed", slog.Any("error", err))
		return "", fmt.Errorf("helm install: %w", err)
	}

	// --- build response ---
	info, err := releaseutil.MapRelease(rel)
	if err != nil {
		log.Warn("failed to map release to response", slog.Any("error", err))
		return "", fmt.Errorf("map release: %w", err)
	}

	log.Debug("helm install command completed",
		slog.String("release", info.Name),
		slog.String("namespace", info.Namespace),
		slog.Int("revision", info.Revision),
	)

	resp := Response{Release: info}

	result, err := bridge.MarshalJSON(resp)
	if err != nil {
		log.Warn("failed to marshal install response", slog.Any("error", err))
		return "", err
	}

	log.Debug("helm install completed successfully")
	return result, nil
}

// applyOptions maps [Options] fields onto the Helm SDK [action.Install]
// client, mirroring the approach used by show.applyOptions.
func applyOptions(client *action.Install, opts Options) {
	// ChartPathOptions
	opts.ChartPathOpts.ApplyTo(&client.ChartPathOptions)

	// Install-specific flags
	client.Devel = opts.Devel
	client.Namespace = opts.Namespace
	client.CreateNamespace = opts.CreateNamespace
	client.WaitForJobs = opts.WaitForJobs
	client.Description = opts.Description
	client.RollbackOnFailure = opts.RollbackOnFailure
	client.SkipCRDs = opts.SkipCRDs
	client.DisableHooks = opts.DisableHooks
	client.DisableOpenAPIValidation = opts.DisableOpenAPIValidation
	client.ForceReplace = opts.ForceReplace
	client.ForceConflicts = opts.ForceConflicts
	if opts.ServerSideApply != nil {
		client.ServerSideApply = *opts.ServerSideApply
	}
	client.Replace = opts.Replace
	client.GenerateName = opts.GenerateName
	client.NameTemplate = opts.NameTemplate
	client.SubNotes = opts.SubNotes
	client.EnableDNS = opts.EnableDNS
	client.TakeOwnership = opts.TakeOwnership
	client.Labels = opts.Labels

	if opts.DryRun != "" {
		client.DryRunStrategy = action.DryRunStrategy(opts.DryRun)
	}

	if opts.Wait != "" {
		client.WaitStrategy = kube.WaitStrategy(opts.Wait)
	}

	if opts.Timeout != "" {
		d, _ := time.ParseDuration(opts.Timeout) // already validated above in Run
		client.Timeout = d
	}
}

