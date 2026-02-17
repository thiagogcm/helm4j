// Package install implements `helm install` — installing a chart into a
// Kubernetes cluster via the Helm SDK action layer.
package install

import (
	"errors"
	"fmt"
	"log/slog"
	"strings"
	"time"

	"helm.sh/helm/v4/pkg/action"
	chart "helm.sh/helm/v4/pkg/chart/v2"
	"helm.sh/helm/v4/pkg/kube"
	"helm.sh/helm/v4/pkg/release"
	v1release "helm.sh/helm/v4/pkg/release/v1"

	"github.com/thiagogcm/libhelm4j/internal/bridge"
	"github.com/thiagogcm/libhelm4j/internal/helmenv"
	"github.com/thiagogcm/libhelm4j/internal/helmlog"
)

// Options captures the Helm flags relevant to `helm install` and chart
// discovery. JSON tags use the same camelCase convention as every other
// operation in libhelm4j.
type Options struct {
	// Chart resolution (ChartPathOptions)
	Version               string `json:"version,omitempty"`
	RepoURL               string `json:"repo,omitempty"`
	Username              string `json:"username,omitempty"`
	Password              string `json:"password,omitempty"`
	PlainHTTP             bool   `json:"plainHttp,omitempty"`
	InsecureSkipTLSVerify bool   `json:"insecureSkipTlsVerify,omitempty"`
	Keyring               string `json:"keyring,omitempty"`
	CertFile              string `json:"certFile,omitempty"`
	KeyFile               string `json:"keyFile,omitempty"`
	CaFile                string `json:"caFile,omitempty"`
	PassCredentialsAll    bool   `json:"passCredentialsAll,omitempty"`
	Verify                bool   `json:"verify,omitempty"`
	Devel                 bool   `json:"devel,omitempty"`

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

	// Values — a pre-merged values map from the caller.
	Values map[string]any    `json:"values,omitempty"`
	Labels map[string]string `json:"labels,omitempty"`
}

// ReleaseInfo is the structured release payload returned on success.
type ReleaseInfo struct {
	Name          string `json:"name"`
	Namespace     string `json:"namespace"`
	Revision      int    `json:"revision"`
	Status        string `json:"status"`
	Description   string `json:"description"`
	FirstDeployed string `json:"firstDeployed"`
	LastDeployed  string `json:"lastDeployed"`
	ChartName     string `json:"chartName"`
	ChartVersion  string `json:"chartVersion"`
	AppVersion    string `json:"appVersion"`
	Notes         string `json:"notes"`
}

// Response is the top-level JSON payload returned across the FFM boundary.
type Response struct {
	Release ReleaseInfo `json:"release"`
}

// Run executes a helm install operation for the given release name and chart
// reference. It returns the JSON-encoded response string or an error.
func Run(releaseName, chartRef string, opts Options) (string, error) {
	log := helmlog.Logger().With(
		slog.String("operation", "install"),
		slog.String("releaseName", releaseName),
		slog.String("chartRef", chartRef),
	)

	if strings.TrimSpace(releaseName) == "" && !opts.GenerateName {
		return "", errors.New("release name is required (or set generateName)")
	}
	if strings.TrimSpace(chartRef) == "" {
		return "", errors.New("chart reference is required")
	}

	log.Debug("running helm install")

	// --- bootstrap environment ---
	env, err := helmenv.New()
	if err != nil {
		log.Warn("failed to initialize helm environment", slog.Any("error", err))
		return "", fmt.Errorf("bootstrap helm: %w", err)
	}

	if opts.Namespace != "" {
		env.Settings.SetNamespace(opts.Namespace)
	}

	regClient, err := helmenv.BuildRegistryClient(env.Settings, helmenv.RegistryOptions{
		CertFile:              opts.CertFile,
		KeyFile:               opts.KeyFile,
		CaFile:                opts.CaFile,
		InsecureSkipTLSVerify: opts.InsecureSkipTLSVerify,
		PlainHTTP:             opts.PlainHTTP,
		Username:              opts.Username,
		Password:              opts.Password,
	})
	if err != nil {
		log.Warn("failed to initialize registry client", slog.Any("error", err))
		return "", fmt.Errorf("registry client: %w", err)
	}
	env.Config.RegistryClient = regClient

	// --- configure the install action ---
	client := action.NewInstall(env.Config)
	client.ReleaseName = releaseName
	client.SetRegistryClient(regClient)
	applyOptions(client, opts)

	// --- locate and load the chart ---
	_, ch, err := helmenv.LocateAndLoadChart(chartRef, client.Version, client.Devel, env.Settings, client)
	if err != nil {
		log.Warn("failed to locate or load chart", slog.Any("error", err))
		return "", err
	}

	// --- run the install ---
	vals := opts.Values
	if vals == nil {
		vals = make(map[string]any)
	}

	rel, err := client.Run(ch, vals)
	if err != nil {
		log.Warn("helm install command failed", slog.Any("error", err))
		return "", fmt.Errorf("helm install: %w", err)
	}

	// --- build response ---
	info, err := mapRelease(rel)
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
	client.ChartPathOptions.Version = opts.Version
	client.ChartPathOptions.RepoURL = opts.RepoURL
	client.ChartPathOptions.Username = opts.Username
	client.ChartPathOptions.Password = opts.Password
	client.ChartPathOptions.PlainHTTP = opts.PlainHTTP
	client.ChartPathOptions.InsecureSkipTLSVerify = opts.InsecureSkipTLSVerify
	client.ChartPathOptions.Keyring = opts.Keyring
	client.ChartPathOptions.CertFile = opts.CertFile
	client.ChartPathOptions.KeyFile = opts.KeyFile
	client.ChartPathOptions.CaFile = opts.CaFile
	client.ChartPathOptions.PassCredentialsAll = opts.PassCredentialsAll
	client.ChartPathOptions.Verify = opts.Verify

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
		if d, err := time.ParseDuration(opts.Timeout); err == nil {
			client.Timeout = d
		}
	}
}

// mapRelease converts a Helm SDK [release.Releaser] into the serialisable
// [ReleaseInfo] returned across the FFM boundary. It uses the [release.Accessor]
// interface for forward-compatible field access, falling back to a type
// assertion for fields only available on the concrete v1 release type.
func mapRelease(rel release.Releaser) (ReleaseInfo, error) {
	acc, err := release.NewAccessor(rel)
	if err != nil {
		return ReleaseInfo{}, fmt.Errorf("create release accessor: %w", err)
	}

	info := ReleaseInfo{
		Name:      acc.Name(),
		Namespace: acc.Namespace(),
		Revision:  acc.Version(),
		Status:    acc.Status(),
		Notes:     acc.Notes(),
	}

	// Chart metadata via type assertion to the concrete v2 chart type.
	if ch := acc.Chart(); ch != nil {
		if v2ch, ok := ch.(*chart.Chart); ok && v2ch.Metadata != nil {
			info.ChartName = v2ch.Metadata.Name
			info.ChartVersion = v2ch.Metadata.Version
			info.AppVersion = v2ch.Metadata.AppVersion
		}
	}

	// Description, FirstDeployed, LastDeployed are only available on the
	// concrete v1 release type.
	if v1, ok := rel.(*v1release.Release); ok && v1.Info != nil {
		info.Description = v1.Info.Description
		if !v1.Info.FirstDeployed.IsZero() {
			info.FirstDeployed = v1.Info.FirstDeployed.UTC().Format(time.RFC3339)
		}
		if !v1.Info.LastDeployed.IsZero() {
			info.LastDeployed = v1.Info.LastDeployed.UTC().Format(time.RFC3339)
		}
	}

	return info, nil
}
