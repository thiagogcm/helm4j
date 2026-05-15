// Package bridge provides the pure-Go side of the CGo boundary: JSON
// marshalling, option parsing, and structured error encoding.
//
// All functions in this package work exclusively with Go strings — the thin
// CGo conversion layer (goString / toCString / FreeString) lives in main.go.
// This design keeps the package fully testable without a CGo toolchain.
package bridge

import (
	"encoding/json"
	"fmt"
	"log/slog"
	"maps"
	"strings"

	"helm.sh/helm/v4/pkg/kube"

	"github.com/thiagogcm/libhelm4j/internal/helmlog"
)

// Pipeline stages reported in error payloads.
const (
	StageParseOptions = "parseOptions"
	StageRun          = "runOperation"
	StageMarshal      = "marshalResponse"
	StagePanic        = "panic"
	StageMarshalError = "marshalError"
)

// OperationError is a structured error payload returned across the FFM
// boundary. It carries an error message, the pipeline stage that failed,
// and an optional Context map for operation-specific metadata (e.g. mode,
// chartRef for show errors).
//
// The JSON representation keeps a flat shape by promoting Context entries to
// top-level fields via a custom MarshalJSON.
type OperationError struct {
	Stage   string            `json:"stage,omitempty"`
	Error   string            `json:"error"`
	Context map[string]string `json:"-"` // promoted to top-level keys
}

// MarshalJSON produces a flat JSON object where each Context key becomes a
// top-level field alongside "stage" and "error". This keeps the on-wire
// format expected by the Java gateway error decoder.
func (e OperationError) MarshalJSON() ([]byte, error) {
	m := make(map[string]string, len(e.Context)+2)
	maps.Copy(m, e.Context)
	if e.Stage != "" {
		m["stage"] = e.Stage
	}
	m["error"] = e.Error
	return json.Marshal(m)
}

// EncodeError serialises an OperationError as a JSON string. The optional
// kvPairs argument accepts alternating key/value strings that are promoted
// to top-level JSON fields (e.g. "mode", "chart", "chartRef", "myChart").
// If marshalling itself fails, a hand-crafted fallback JSON is returned so
// the caller always gets a valid payload.
func EncodeError(stage string, err error, kvPairs ...string) string {
	var ctx map[string]string
	if n := len(kvPairs); n >= 2 {
		ctx = make(map[string]string, n/2)
		for i := 0; i+1 < n; i += 2 {
			ctx[kvPairs[i]] = kvPairs[i+1]
		}
	}

	payload, marshalErr := MarshalJSON(OperationError{
		Stage:   stage,
		Error:   err.Error(),
		Context: ctx,
	})
	if marshalErr != nil {
		helmlog.Logger().Error(
			"failed to encode error payload",
			slog.String("stage", stage),
			slog.Any("error", marshalErr),
		)
		return `{"error":"failed to encode error payload","stage":"` + StageMarshalError + `"}`
	}
	return payload
}

// ValidateWaitStrategy returns an error when the supplied wire value is not
// one of the accepted Helm v4 kube wait strategies. Empty input is valid
// (Helm picks the default). Strategy names are sourced from kube.WaitStrategy
// constants so an upstream rename is caught at compile time.
func ValidateWaitStrategy(value string) error {
	switch kube.WaitStrategy(value) {
	case "", kube.StatusWatcherStrategy, kube.LegacyStrategy, kube.HookOnlyStrategy:
		return nil
	default:
		return fmt.Errorf("invalid wait strategy %q: must be one of %q, %q, %q",
			value, kube.StatusWatcherStrategy, kube.LegacyStrategy, kube.HookOnlyStrategy)
	}
}

// ParseOptions unmarshals a JSON string into the target options type T.
// An empty or whitespace-only input returns the zero value of T (no error).
func ParseOptions[T any](raw string) (T, error) {
	var zero T
	if strings.TrimSpace(raw) == "" {
		return zero, nil
	}

	var opts T
	if err := json.Unmarshal([]byte(raw), &opts); err != nil {
		return zero, fmt.Errorf("decode options: %w", err)
	}
	return opts, nil
}

// MarshalJSON serialises any value to a JSON string. It is a convenience
// wrapper around [encoding/json.Marshal].
func MarshalJSON(value any) (string, error) {
	b, err := json.Marshal(value)
	if err != nil {
		return "", err
	}
	return string(b), nil
}

// Run is the canonical operation pipeline: parse JSON options into T, invoke
// op, and return either the JSON result string or an encoded error payload.
// kvPairs are promoted to top-level fields in error payloads as context.
//
// The returned string is always a valid JSON document — either the operation's
// own response or an OperationError envelope produced by EncodeError.
func Run[T any](raw string, op func(T) (string, error), kvPairs ...string) string {
	opts, err := ParseOptions[T](raw)
	if err != nil {
		return EncodeError(StageParseOptions, err, kvPairs...)
	}
	result, err := op(opts)
	if err != nil {
		return EncodeError(StageRun, err, kvPairs...)
	}
	return result
}
