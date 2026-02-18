// Package upgrade implements `helm upgrade` — upgrading an existing release
// to a new chart version or with new values.
package upgrade

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
	"github.com/thiagogcm/libhelm4j/internal/releaseutil"
)

// Options captures the Helm flags relevant to `helm upgrade`.
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

	log.Debug("running helm upgrade")

	env, err := helmenv.New()
	if err != nil {
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
		return "", fmt.Errorf("registry client: %w", err)
	}
	env.Config.RegistryClient = regClient

	client := action.NewUpgrade(env.Config)
	client.SetRegistryClient(regClient)
	applyOptions(client, opts)

	_, ch, err := helmenv.LocateAndLoadChart(chartRef, client.Version, client.Devel, env.Settings, client)
	if err != nil {
		return "", err
	}

	vals := opts.Values
	if vals == nil {
		vals = make(map[string]any)
	}

	rel, err := client.Run(releaseName, ch, vals)
	if err != nil {
		return "", fmt.Errorf("helm upgrade: %w", err)
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

	log.Debug("helm upgrade completed successfully")
	return result, nil
}

func applyOptions(client *action.Upgrade, opts Options) {
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
		if *opts.ServerSideApply {
			client.ServerSideApply = "true"
		} else {
			client.ServerSideApply = "false"
		}
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
		if d, err := time.ParseDuration(opts.Timeout); err == nil {
			client.Timeout = d
		}
	}
}
