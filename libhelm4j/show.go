package main

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
)

// ShowOptions captures the Helm flags relevant to `helm show` and chart discovery.
type ShowOptions struct {
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

// ShowSections mirrors the logical sections printed by the Helm CLI.
type ShowSections struct {
	Chart  string   `json:"chart,omitempty"`
	Values string   `json:"values,omitempty"`
	Readme string   `json:"readme,omitempty"`
	Crds   []string `json:"crds,omitempty"`
}

// ShowResponse is the structured payload returned on success.
type ShowResponse struct {
	Mode      string       `json:"mode"`
	ChartRef  string       `json:"chartRef"`
	ChartPath string       `json:"chartPath"`
	Sections  ShowSections `json:"sections"`
	CLIOutput string       `json:"cliOutput"`
}

// ShowError is serialized when a runner fails.
type ShowError struct {
	Mode      string `json:"mode"`
	ChartRef  string `json:"chartRef"`
	ChartPath string `json:"chartPath,omitempty"`
	Stage     string `json:"stage,omitempty"`
	Error     string `json:"error"`
}

func runShow(mode action.ShowOutputFormat, chartRef string, opts ShowOptions) (string, error) {
	showLog := nativeLogger.With(
		slog.String("operation", "show"),
		slog.String("mode", mode.String()),
		slog.String("chartRef", chartRef),
	)

	if strings.TrimSpace(chartRef) == "" {
		return "", errors.New("chart reference is required")
	}

	showLog.Debug("running helm show")

	env, err := newHelmEnv()
	if err != nil {
		showLog.Warn("failed to initialize helm environment", slog.Any("error", err))
		return "", fmt.Errorf("bootstrap helm: %w", err)
	}

	regClient, err := buildRegistryClient(env.Settings, registryOptions{
		CertFile:              opts.CertFile,
		KeyFile:               opts.KeyFile,
		CaFile:                opts.CaFile,
		InsecureSkipTLSVerify: opts.InsecureSkipTLSVerify,
		PlainHTTP:             opts.PlainHTTP,
		Username:              opts.Username,
		Password:              opts.Password,
	})
	if err != nil {
		showLog.Warn("failed to initialize registry client", slog.Any("error", err))
		return "", fmt.Errorf("registry client: %w", err)
	}
	env.Config.RegistryClient = regClient

	client := action.NewShow(mode, env.Config)
	applyShowOptions(client, opts)
	client.SetRegistryClient(regClient)

	chartPath, ch, err := locateAndLoadChart(client, env.Settings, chartRef)
	if err != nil {
		showLog.Warn("failed to locate or load chart", slog.Any("error", err))
		return "", err
	}

	sections, err := buildShowSections(client, ch)
	if err != nil {
		showLog.Warn("failed to build show sections", slog.Any("error", err))
		return "", err
	}

	cliOut, err := client.Run(chartPath)
	if err != nil {
		showLog.Warn("helm show command failed", slog.Any("error", err))
		return "", fmt.Errorf("helm show: %w", err)
	}

	showLog.Debug(
		"helm show command completed",
		slog.String("chartPath", chartPath),
		slog.Int("cliOutputLength", len(cliOut)),
	)

	resp := ShowResponse{
		Mode:      mode.String(),
		ChartRef:  chartRef,
		ChartPath: chartPath,
		Sections:  sections,
		CLIOutput: cliOut,
	}

	result, err := marshalJSON(resp)
	if err != nil {
		showLog.Warn("failed to marshal show response", slog.Any("error", err))
		return "", err
	}

	showLog.Debug("helm show completed successfully")

	return result, nil
}

func applyShowOptions(client *action.Show, opts ShowOptions) {
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

func buildShowSections(client *action.Show, ch *chart.Chart) (ShowSections, error) {
	sections := ShowSections{}

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

func findReadmeFile(files []*common.File) (file *common.File) {
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
