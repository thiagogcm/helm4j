// Package template implements `helm template` — client-side chart
// rendering without contacting a Kubernetes cluster.
package template

import (
	"errors"
	"fmt"
	"log/slog"
	"strings"

	"helm.sh/helm/v4/pkg/action"
	"helm.sh/helm/v4/pkg/release"

	"github.com/thiagogcm/libhelm4j/internal/bridge"
	"github.com/thiagogcm/libhelm4j/internal/helmenv"
	"github.com/thiagogcm/libhelm4j/internal/helmlog"
	"github.com/thiagogcm/libhelm4j/internal/releaseutil"
)

// Options captures the Helm flags relevant to `helm template`.
type Options struct {
	// Chart resolution
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

	// Template behaviour
	Namespace                string            `json:"namespace,omitempty"`
	Description              string            `json:"description,omitempty"`
	SkipCRDs                 bool              `json:"skipCrds,omitempty"`
	DisableHooks             bool              `json:"disableHooks,omitempty"`
	DisableOpenAPIValidation bool              `json:"disableOpenApiValidation,omitempty"`
	GenerateName             bool              `json:"generateName,omitempty"`
	NameTemplate             string            `json:"nameTemplate,omitempty"`
	SubNotes                 bool              `json:"subNotes,omitempty"`
	EnableDNS                bool              `json:"enableDns,omitempty"`
	IncludeCRDs              bool              `json:"includeCrds,omitempty"`
	APIVersions              []string          `json:"apiVersions,omitempty"`
	Values                   map[string]any    `json:"values,omitempty"`
	Labels                   map[string]string `json:"labels,omitempty"`
}

// Response is the top-level JSON payload returned across the FFM boundary.
type Response struct {
	Release  releaseutil.ReleaseInfo `json:"release"`
	Manifest string                  `json:"manifest"`
}

// Run executes a helm template operation for the given release name and chart
// reference. It returns the JSON-encoded response string or an error.
func Run(releaseName, chartRef string, opts Options) (string, error) {
	log := helmlog.Logger().With(
		slog.String("operation", "template"),
		slog.String("releaseName", releaseName),
		slog.String("chartRef", chartRef),
	)

	if strings.TrimSpace(releaseName) == "" && !opts.GenerateName {
		return "", errors.New("release name is required (or set generateName)")
	}
	if strings.TrimSpace(chartRef) == "" {
		return "", errors.New("chart reference is required")
	}

	log.Debug("running helm template")

	env, err := helmenv.NewWithNamespace(opts.Namespace)
	if err != nil {
		return "", fmt.Errorf("bootstrap helm: %w", err)
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

	client := action.NewInstall(env.Config)
	client.ReleaseName = releaseName
	client.SetRegistryClient(regClient)
	client.DryRunStrategy = action.DryRunStrategy("client")
	applyOptions(client, opts)

	_, ch, err := helmenv.LocateAndLoadChart(chartRef, client.Version, client.Devel, env.Settings, client)
	if err != nil {
		return "", err
	}

	vals := opts.Values
	if vals == nil {
		vals = make(map[string]any)
	}

	rel, err := client.Run(ch, vals)
	if err != nil {
		return "", fmt.Errorf("helm template: %w", err)
	}

	info, err := releaseutil.MapRelease(rel)
	if err != nil {
		return "", fmt.Errorf("map release: %w", err)
	}

	manifest := ""
	if acc, accErr := release.NewAccessor(rel); accErr == nil {
		manifest = acc.Manifest()
	}

	resp := Response{Release: info, Manifest: manifest}
	result, err := bridge.MarshalJSON(resp)
	if err != nil {
		return "", err
	}

	log.Debug("helm template completed successfully")
	return result, nil
}

func applyOptions(client *action.Install, opts Options) {
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
	client.Description = opts.Description
	client.SkipCRDs = opts.SkipCRDs
	client.DisableHooks = opts.DisableHooks
	client.DisableOpenAPIValidation = opts.DisableOpenAPIValidation
	client.GenerateName = opts.GenerateName
	client.NameTemplate = opts.NameTemplate
	client.SubNotes = opts.SubNotes
	client.EnableDNS = opts.EnableDNS
	client.IncludeCRDs = opts.IncludeCRDs
	client.Labels = opts.Labels

	if len(opts.APIVersions) > 0 {
		client.APIVersions = opts.APIVersions
	}
}
