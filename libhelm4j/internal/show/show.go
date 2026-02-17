// Package show implements the `helm show` family of operations
// (chart, values, readme, crds, all). Each variant shares the same
// option struct, section builder, and response shape.
package show

import (
	"errors"
	"fmt"
	"log/slog"
	"strings"

	"go.yaml.in/yaml/v3"
	"helm.sh/helm/v4/pkg/action"
	"helm.sh/helm/v4/pkg/chart/common"
	chart "helm.sh/helm/v4/pkg/chart/v2"
	chartutil "helm.sh/helm/v4/pkg/chart/v2/util"
	"k8s.io/cli-runtime/pkg/printers"

	"github.com/thiagogcm/libhelm4j/internal/bridge"
	"github.com/thiagogcm/libhelm4j/internal/helmenv"
	"github.com/thiagogcm/libhelm4j/internal/helmlog"
)

// Options captures the Helm flags relevant to `helm show` and chart discovery.
type Options struct {
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
	JSONPathTemplate      string `json:"jsonpath,omitempty"`
}

// Sections mirrors the logical sections printed by the Helm CLI.
type Sections struct {
	Chart  string   `json:"chart,omitempty"`
	Values string   `json:"values,omitempty"`
	Readme string   `json:"readme,omitempty"`
	Crds   []string `json:"crds,omitempty"`
}

// Response is the structured payload returned on success.
type Response struct {
	Mode      string   `json:"mode"`
	ChartRef  string   `json:"chartRef"`
	ChartPath string   `json:"chartPath"`
	Sections  Sections `json:"sections"`
	CLIOutput string   `json:"cliOutput"`
}

// Run executes a helm show operation for the given mode and chart reference.
// It returns the JSON-encoded response string or an error.
func Run(mode action.ShowOutputFormat, chartRef string, opts Options) (string, error) {
	log := helmlog.Logger().With(
		slog.String("operation", "show"),
		slog.String("mode", mode.String()),
		slog.String("chartRef", chartRef),
	)

	if strings.TrimSpace(chartRef) == "" {
		return "", errors.New("chart reference is required")
	}

	log.Debug("running helm show")

	env, err := helmenv.New()
	if err != nil {
		log.Warn("failed to initialize helm environment", slog.Any("error", err))
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
		log.Warn("failed to initialize registry client", slog.Any("error", err))
		return "", fmt.Errorf("registry client: %w", err)
	}
	env.Config.RegistryClient = regClient

	client := action.NewShow(mode, env.Config)
	applyOptions(client, opts)
	client.SetRegistryClient(regClient)

	chartPath, ch, err := helmenv.LocateAndLoadChart(chartRef, client.Version, client.Devel, env.Settings, client)
	if err != nil {
		log.Warn("failed to locate or load chart", slog.Any("error", err))
		return "", err
	}

	sections, err := buildSections(client, ch)
	if err != nil {
		log.Warn("failed to build show sections", slog.Any("error", err))
		return "", err
	}

	cliOut, err := client.Run(chartPath)
	if err != nil {
		log.Warn("helm show command failed", slog.Any("error", err))
		return "", fmt.Errorf("helm show: %w", err)
	}

	log.Debug(
		"helm show command completed",
		slog.String("chartPath", chartPath),
		slog.Int("cliOutputLength", len(cliOut)),
	)

	resp := Response{
		Mode:      mode.String(),
		ChartRef:  chartRef,
		ChartPath: chartPath,
		Sections:  sections,
		CLIOutput: cliOut,
	}

	result, err := bridge.MarshalJSON(resp)
	if err != nil {
		log.Warn("failed to marshal show response", slog.Any("error", err))
		return "", err
	}

	log.Debug("helm show completed successfully")
	return result, nil
}

func applyOptions(client *action.Show, opts Options) {
	client.Devel = opts.Devel
	client.JSONPathTemplate = opts.JSONPathTemplate

	client.ChartPathOptions.CaFile = opts.CaFile
	client.ChartPathOptions.CertFile = opts.CertFile
	client.ChartPathOptions.KeyFile = opts.KeyFile
	client.ChartPathOptions.InsecureSkipTLSVerify = opts.InsecureSkipTLSVerify
	client.ChartPathOptions.PlainHTTP = opts.PlainHTTP
	client.ChartPathOptions.Keyring = opts.Keyring
	client.ChartPathOptions.Password = opts.Password
	client.ChartPathOptions.PassCredentialsAll = opts.PassCredentialsAll
	client.ChartPathOptions.RepoURL = opts.RepoURL
	client.ChartPathOptions.Username = opts.Username
	client.ChartPathOptions.Verify = opts.Verify
	client.ChartPathOptions.Version = opts.Version
}

func buildSections(client *action.Show, ch *chart.Chart) (Sections, error) {
	sections := Sections{}

	if client.OutputFormat == action.ShowChart || client.OutputFormat == action.ShowAll {
		meta, err := yaml.Marshal(ch.Metadata)
		if err != nil {
			return sections, fmt.Errorf("marshal chart metadata: %w", err)
		}
		sections.Chart = string(meta)
	}

	if (client.OutputFormat == action.ShowValues || client.OutputFormat == action.ShowAll) && ch.Values != nil {
		if client.JSONPathTemplate != "" {
			printer, err := printers.NewJSONPathPrinter(client.JSONPathTemplate)
			if err != nil {
				return sections, fmt.Errorf("parse jsonpath: %w", err)
			}

			var buf strings.Builder
			if err := printer.Execute(&buf, ch.Values); err != nil {
				return sections, fmt.Errorf("run jsonpath: %w", err)
			}
			sections.Values = buf.String()
		} else {
			for _, f := range ch.Raw {
				if f.Name == chartutil.ValuesfileName {
					sections.Values = string(f.Data)
					break
				}
			}
		}
	}

	if client.OutputFormat == action.ShowReadme || client.OutputFormat == action.ShowAll {
		if readme := findReadmeFile(ch.Files); readme != nil {
			sections.Readme = string(readme.Data)
		}
	}

	if client.OutputFormat == action.ShowCRDs || client.OutputFormat == action.ShowAll {
		crds := ch.CRDObjects()
		if len(crds) > 0 {
			sections.Crds = make([]string, 0, len(crds))
			for _, crd := range crds {
				sections.Crds = append(sections.Crds, string(crd.File.Data))
			}
		}
	}

	return sections, nil
}

func findReadmeFile(files []*common.File) *common.File {
	for _, file := range files {
		if file == nil {
			continue
		}
		if _, ok := readmeFileNames[strings.ToLower(file.Name)]; ok {
			return file
		}
	}
	return nil
}

var readmeFileNames = map[string]struct{}{
	"readme.md":  {},
	"readme.txt": {},
	"readme":     {},
}
