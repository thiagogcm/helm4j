// Package lint implements `helm lint` — static analysis of Helm charts
// for best practices and potential issues.
package lint

import (
	"fmt"
	"log/slog"
	"strings"

	"helm.sh/helm/v4/pkg/action"

	"github.com/thiagogcm/libhelm4j/internal/bridge"
	"github.com/thiagogcm/libhelm4j/internal/helmlog"
)

// Options captures the Helm flags relevant to `helm lint`.
type Options struct {
	Strict        bool           `json:"strict,omitempty"`
	Quiet         bool           `json:"quiet,omitempty"`
	WithSubcharts bool           `json:"withSubcharts,omitempty"`
	Values        map[string]any `json:"values,omitempty"`
}

// Message represents a single lint finding.
type Message struct {
	Severity string `json:"severity"`
	Message  string `json:"message"`
}

// Response is the top-level JSON payload returned across the FFM boundary.
type Response struct {
	Messages     []Message `json:"messages"`
	TotalCharts  int       `json:"totalCharts"`
	ChartsTested int       `json:"chartsTested"`
	ChartsFailed int       `json:"chartsFailed"`
}

// Run executes a helm lint operation for the given chart path.
// It returns the JSON-encoded response string or an error.
func Run(chartPath string, opts Options) (string, error) {
	log := helmlog.Logger().With(
		slog.String("operation", "lint"),
		slog.String("chartPath", chartPath),
	)

	if strings.TrimSpace(chartPath) == "" {
		return "", fmt.Errorf("chart path is required")
	}

	log.Debug("running helm lint")

	client := action.NewLint()
	client.Strict = opts.Strict
	client.Quiet = opts.Quiet
	client.WithSubcharts = opts.WithSubcharts

	vals := opts.Values
	if vals == nil {
		vals = make(map[string]any)
	}

	result := client.Run([]string{chartPath}, vals)

	var messages []Message
	for _, msg := range result.Messages {
		messages = append(messages, Message{
			Severity: mapSeverity(msg.Severity),
			Message:  msg.Err.Error(),
		})
	}
	if messages == nil {
		messages = []Message{}
	}

	failedCount := 0
	for _, e := range result.Errors {
		if e != nil {
			failedCount++
		}
	}

	// Helm v4's lint.Result only carries TotalChartsLinted; "tested" mirrors that
	// figure so the Java LintResult exposes both counters without parity drift.
	resp := Response{
		Messages:     messages,
		TotalCharts:  result.TotalChartsLinted,
		ChartsTested: result.TotalChartsLinted,
		ChartsFailed: failedCount,
	}

	out, err := bridge.MarshalJSON(resp)
	if err != nil {
		return "", err
	}

	log.Debug("helm lint completed", slog.Int("messages", len(messages)))
	return out, nil
}

func mapSeverity(severity int) string {
	switch severity {
	case 0:
		return "UNKNOWN"
	case 1:
		return "INFO"
	case 2:
		return "WARNING"
	case 3:
		return "ERROR"
	default:
		return "UNKNOWN"
	}
}
