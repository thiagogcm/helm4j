// Package upgrade implements `helm upgrade` — upgrading an existing release
// to a new chart version or with new values.
package upgrade

import (
	"context"
	"errors"
	"fmt"
	"log/slog"
	"strconv"
	"strings"
	"time"

	"helm.sh/helm/v4/pkg/action"
	"helm.sh/helm/v4/pkg/chart"
	"helm.sh/helm/v4/pkg/downloader"
	"helm.sh/helm/v4/pkg/getter"
	"helm.sh/helm/v4/pkg/kube"
	"helm.sh/helm/v4/pkg/storage/driver"

	"github.com/thiagogcm/libhelm4j/internal/bridge"
	"github.com/thiagogcm/libhelm4j/internal/helmenv"
	"github.com/thiagogcm/libhelm4j/internal/helmlog"
	"github.com/thiagogcm/libhelm4j/internal/releaseutil"
)

// Options captures the Helm flags relevant to `helm upgrade`.
type Options struct {
	// Chart resolution (shared with install, template, show, pull)
	helmenv.ChartPathOpts

	// Upgrade behaviour
	Namespace                string `json:"namespace,omitempty"`
	Install                  bool   `json:"install,omitempty"`
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
	SubNotes                 bool   `json:"subNotes,omitempty"`
	EnableDNS                bool   `json:"enableDns,omitempty"`
	TakeOwnership            bool   `json:"takeOwnership,omitempty"`
	CleanupOnFail            bool   `json:"cleanupOnFail,omitempty"`
	MaxHistory               int    `json:"maxHistory,omitempty"`
	ReuseValues              bool   `json:"reuseValues,omitempty"`
	ResetValues              bool   `json:"resetValues,omitempty"`
	ResetThenReuseValues     bool   `json:"resetThenReuseValues,omitempty"`
	DependencyUpdate         bool   `json:"dependencyUpdate,omitempty"`

	Values map[string]any    `json:"values,omitempty"`
	Labels map[string]string `json:"labels,omitempty"`
}

// Response is the top-level JSON payload returned across the FFM boundary.
type Response struct {
	Release releaseutil.ReleaseInfo `json:"release"`
}

// Run executes a helm upgrade operation for the given release name and chart
// reference. It returns the JSON-encoded response string or an error.
func Run(releaseName, chartRef string, opts Options) (string, error) {
	log := helmlog.Logger().With(
		slog.String("operation", "upgrade"),
		slog.String("releaseName", releaseName),
		slog.String("chartRef", chartRef),
	)

	if strings.TrimSpace(releaseName) == "" {
		return "", errors.New("release name is required")
	}
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

	log.Debug("running helm upgrade")

	env, err := helmenv.NewWithNamespace(opts.Namespace)
	if err != nil {
		return "", fmt.Errorf("bootstrap helm: %w", err)
	}

	regClient, err := helmenv.BuildRegistryClient(env.Settings, helmenv.RegistryOptsFromChartPath(opts.ChartPathOpts))
	if err != nil {
		return "", fmt.Errorf("registry client: %w", err)
	}
	env.Config.RegistryClient = regClient

	client := action.NewUpgrade(env.Config)
	client.SetRegistryClient(regClient)
	applyOptions(client, opts)

	chartPath, ch, err := helmenv.LocateAndLoadChart(chartRef, client.Version, client.Devel, env.Settings, client)
	if err != nil {
		return "", err
	}

	// --- dependency preflight ---
	ch, err = checkDependencies(ch, chartPath, chartRef, client, env, opts, log)
	if err != nil {
		return "", err
	}

	vals := opts.Values
	if vals == nil {
		vals = make(map[string]any)
	}

	ctx := context.Background()
	if client.Timeout > 0 {
		var cancel context.CancelFunc
		ctx, cancel = context.WithTimeout(ctx, client.Timeout)
		defer cancel()
	}

	// When install=true, check release history and fall back to install
	// if the release does not exist. The Upgrade.Install field in the Helm
	// SDK is informational only — the actual fallback must be done here,
	// mirroring what the Helm CLI does in pkg/cmd/upgrade.go.
	if opts.Install {
		rel, fallbackErr := installFallback(ctx, env, client, releaseName, ch, vals, opts)
		if fallbackErr == nil {
			// Fallback to install succeeded — return the install result.
			if rel != nil {
				return marshalResponse(rel)
			}
			// rel == nil means history exists, proceed with normal upgrade.
		} else {
			return "", fallbackErr
		}
	}

	rel, err := client.RunWithContext(ctx, releaseName, ch, vals)
	if err != nil {
		return "", fmt.Errorf("helm upgrade: %w", err)
	}

	return marshalResponse(rel)
}

func marshalResponse(rel any) (string, error) {
	info, err := releaseutil.MapRelease(rel)
	if err != nil {
		return "", fmt.Errorf("map release: %w", err)
	}

	resp := Response{Release: info}
	return bridge.MarshalJSON(resp)
}

// installFallback checks whether the release already exists. If it does not
// (or is fully uninstalled), it performs an install and returns the resulting
// release. If the release exists, it returns (nil, nil) to signal the caller
// should proceed with a normal upgrade.
func installFallback(ctx context.Context, env *helmenv.Env, upgradeClient *action.Upgrade, releaseName string, ch chart.Charter, vals map[string]any, opts Options) (any, error) {
	hist := action.NewHistory(env.Config)
	hist.Max = 1
	versions, histErr := hist.Run(releaseName)

	needInstall := false
	if histErr != nil {
		if errors.Is(histErr, driver.ErrReleaseNotFound) {
			needInstall = true
		} else {
			return nil, fmt.Errorf("history lookup: %w", histErr)
		}
	} else if len(versions) > 0 && releaseutil.IsAllUninstalled(versions) {
		needInstall = true
	}

	if !needInstall {
		// Release exists — caller should proceed with normal upgrade.
		return nil, nil
	}

	inst := action.NewInstall(env.Config)
	inst.SetRegistryClient(env.Config.RegistryClient)
	mapUpgradeToInstall(inst, upgradeClient, opts)
	inst.ReleaseName = releaseName

	rel, err := inst.RunWithContext(ctx, ch, vals)
	if err != nil {
		return nil, fmt.Errorf("helm install (via upgrade --install): %w", err)
	}
	return rel, nil
}

// mapUpgradeToInstall copies applicable upgrade options to an install action.
func mapUpgradeToInstall(inst *action.Install, upgrade *action.Upgrade, opts Options) {
	inst.ChartPathOptions = upgrade.ChartPathOptions
	inst.DryRunStrategy = upgrade.DryRunStrategy
	inst.WaitStrategy = upgrade.WaitStrategy
	inst.WaitForJobs = upgrade.WaitForJobs
	inst.Devel = upgrade.Devel
	inst.Namespace = upgrade.Namespace
	inst.Timeout = upgrade.Timeout
	inst.Description = upgrade.Description
	inst.SkipCRDs = upgrade.SkipCRDs
	inst.DisableHooks = upgrade.DisableHooks
	inst.DisableOpenAPIValidation = upgrade.DisableOpenAPIValidation
	inst.SubNotes = upgrade.SubNotes
	inst.EnableDNS = upgrade.EnableDNS
	inst.TakeOwnership = upgrade.TakeOwnership
	inst.Labels = upgrade.Labels
	inst.CreateNamespace = true
}

func applyOptions(client *action.Upgrade, opts Options) {
	helmenv.ApplyChartPathOptions(&client.ChartPathOptions, opts.ChartPathOpts)

	client.Devel = opts.Devel
	client.Namespace = opts.Namespace
	client.Install = opts.Install
	client.WaitForJobs = opts.WaitForJobs
	client.Description = opts.Description
	client.RollbackOnFailure = opts.RollbackOnFailure
	client.SkipCRDs = opts.SkipCRDs
	client.DisableHooks = opts.DisableHooks
	client.DisableOpenAPIValidation = opts.DisableOpenAPIValidation
	client.ForceReplace = opts.ForceReplace
	client.ForceConflicts = opts.ForceConflicts
	if opts.ServerSideApply != nil {
		client.ServerSideApply = strconv.FormatBool(*opts.ServerSideApply)
	}
	client.SubNotes = opts.SubNotes
	client.EnableDNS = opts.EnableDNS
	client.TakeOwnership = opts.TakeOwnership
	client.CleanupOnFail = opts.CleanupOnFail
	client.MaxHistory = opts.MaxHistory
	client.ReuseValues = opts.ReuseValues
	client.ResetValues = opts.ResetValues
	client.ResetThenReuseValues = opts.ResetThenReuseValues
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

// checkDependencies verifies the chart's dependencies are present. When
// opts.DependencyUpdate is true and dependencies are missing, it downloads
// them and reloads the chart. Otherwise it returns an error.
func checkDependencies(ch any, chartPath, chartRef string, client *action.Upgrade, env *helmenv.Env, opts Options, log *slog.Logger) (any, error) {
	chAcc, err := chart.NewAccessor(ch)
	if err != nil {
		return ch, nil
	}

	deps := chAcc.MetaDependencies()
	if len(deps) == 0 {
		return ch, nil
	}

	if err := action.CheckDependencies(ch, deps); err != nil {
		if !opts.DependencyUpdate {
			return nil, fmt.Errorf("missing chart dependencies: %w; run 'helm dependency build' or set dependencyUpdate", err)
		}

		log.Debug("updating chart dependencies", slog.String("chartPath", chartPath))
		man := &downloader.Manager{
			ChartPath:        chartPath,
			Getters:          getter.All(env.Settings),
			RegistryClient:   env.Config.RegistryClient,
			RepositoryConfig: env.Settings.RepositoryConfig,
			RepositoryCache:  env.Settings.RepositoryCache,
			ContentCache:     env.Settings.ContentCache,
			Debug:            env.Settings.Debug,
		}
		if err := man.Update(); err != nil {
			return nil, fmt.Errorf("update dependencies: %w", err)
		}

		_, reloaded, err := helmenv.LocateAndLoadChart(chartRef, client.Version, client.Devel, env.Settings, client)
		if err != nil {
			return nil, fmt.Errorf("reload chart after dependency update: %w", err)
		}
		return reloaded, nil
	}

	return ch, nil
}
