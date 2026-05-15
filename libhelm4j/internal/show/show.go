// Package show implements the `helm show` family of operations
// (chart, values, readme, crds, all). Each variant shares the same
// option struct, section builder, and response shape.
package show

import (
	"errors"
	"fmt"
	"log/slog"
	"slices"
	"strings"

	"go.yaml.in/yaml/v3"
	"helm.sh/helm/v4/pkg/action"
	"helm.sh/helm/v4/pkg/chart"
	"helm.sh/helm/v4/pkg/chart/common"
	v2chart "helm.sh/helm/v4/pkg/chart/v2"
	chartutil "helm.sh/helm/v4/pkg/chart/v2/util"
	"k8s.io/cli-runtime/pkg/printers"

	"github.com/thiagogcm/libhelm4j/internal/bridge"
	"github.com/thiagogcm/libhelm4j/internal/helmenv"
	"github.com/thiagogcm/libhelm4j/internal/helmlog"
)

// Options captures the Helm flags relevant to `helm show` and chart discovery.
type Options struct {
	// Chart resolution (shared with install, upgrade, template, pull)
	helmenv.ChartPathOpts

	// Show-specific
	JSONPathTemplate string `json:"jsonpath,omitempty"`
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

	regClient, err := helmenv.BuildRegistryClient(env.Settings, opts.ChartPathOpts.RegistryOptions())
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
	opts.ChartPathOpts.ApplyTo(&client.ChartPathOptions)
}

func buildSections(client *action.Show, ch chart.Charter) (Sections, error) {
	sections := Sections{}

	v2ch, _ := ch.(*v2chart.Chart)

	if client.OutputFormat == action.ShowChart || client.OutputFormat == action.ShowAll {
		if v2ch != nil && v2ch.Metadata != nil {
			meta, err := yaml.Marshal(v2ch.Metadata)
			if err != nil {
				return sections, fmt.Errorf("marshal chart metadata: %w", err)
			}
			sections.Chart = string(meta)
		}
	}

	if chAcc, err := chart.NewAccessor(ch); err == nil {
		if (client.OutputFormat == action.ShowValues || client.OutputFormat == action.ShowAll) && chAcc.Values() != nil {
			if client.JSONPathTemplate != "" {
				printer, err := printers.NewJSONPathPrinter(client.JSONPathTemplate)
				if err != nil {
					return sections, fmt.Errorf("parse jsonpath: %w", err)
				}
				var buf strings.Builder
				if err := printer.Execute(&buf, chAcc.Values()); err != nil {
					return sections, fmt.Errorf("run jsonpath: %w", err)
				}
				sections.Values = buf.String()
			} else if v2ch != nil {
				// Raw files (values.yaml with comments) are only available on v2 chart.
				for _, f := range v2ch.Raw {
					if f.Name == chartutil.ValuesfileName {
						sections.Values = string(f.Data)
						break
					}
				}
			}
		}

		if client.OutputFormat == action.ShowReadme || client.OutputFormat == action.ShowAll {
			if readme := findReadmeFile(chAcc.Files()); readme != nil {
				sections.Readme = string(readme.Data)
			}
		}
	}

	if (client.OutputFormat == action.ShowCRDs || client.OutputFormat == action.ShowAll) && v2ch != nil {
		crds := v2ch.CRDObjects()
		if len(crds) > 0 {
			sections.Crds = make([]string, 0, len(crds))
			for _, crd := range crds {
				sections.Crds = append(sections.Crds, string(crd.File.Data))
			}
		}
	}

	return sections, nil
}

// readmeFileNames lists the case-insensitive filenames that count as a README
// when scanning a chart's file list.
var readmeFileNames = []string{"readme.md", "readme.txt", "readme"}

func findReadmeFile(files []*common.File) *common.File {
	for _, file := range files {
		if file == nil {
			continue
		}
		if slices.Contains(readmeFileNames, strings.ToLower(file.Name)) {
			return file
		}
	}
	return nil
}
