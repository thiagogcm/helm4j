package main

/*
#include <stdlib.h>
*/
import "C"

import (
	"encoding/json"
	"fmt"
	"log/slog"
	"runtime/debug"
	"strings"
	"unsafe"

	"helm.sh/helm/v4/pkg/action"
)

type SearchError struct {
	Stage string `json:"stage,omitempty"`
	Error string `json:"error"`
}

const (
	stageParseOptions   = "parseOptions"
	stageRunShow        = "runShow"
	stageRunSearch      = "runSearch"
	stageMarshalPayload = "marshalResponse"
	stagePanic          = "panic"
	stageMarshalError   = "marshalError"
)

//export FreeString
func FreeString(str *C.char) {
	C.free(unsafe.Pointer(str))
}

//export HelmShowChart
func HelmShowChart(chartRef *C.char, options *C.char) (result *C.char) {
	defer recoverShowPanic(&result, action.ShowChart, chartRef)
	return dispatchShow(action.ShowChart, chartRef, options)
}

//export HelmShowValues
func HelmShowValues(chartRef *C.char, options *C.char) (result *C.char) {
	defer recoverShowPanic(&result, action.ShowValues, chartRef)
	return dispatchShow(action.ShowValues, chartRef, options)
}

//export HelmShowReadme
func HelmShowReadme(chartRef *C.char, options *C.char) (result *C.char) {
	defer recoverShowPanic(&result, action.ShowReadme, chartRef)
	return dispatchShow(action.ShowReadme, chartRef, options)
}

//export HelmShowAll
func HelmShowAll(chartRef *C.char, options *C.char) (result *C.char) {
	defer recoverShowPanic(&result, action.ShowAll, chartRef)
	return dispatchShow(action.ShowAll, chartRef, options)
}

//export HelmShowCRDs
func HelmShowCRDs(chartRef *C.char, options *C.char) (result *C.char) {
	defer recoverShowPanic(&result, action.ShowCRDs, chartRef)
	return dispatchShow(action.ShowCRDs, chartRef, options)
}

//export HelmSearch
func HelmSearch(options *C.char) (result *C.char) {
	defer recoverSearchPanic(&result)

	searchLog := nativeLogger.With(slog.String("operation", "search"))
	searchLog.Debug("handling native helm search request")

	goOptions := goString(options)
	opts, err := parseOptions[SearchOptions](goOptions)
	if err != nil {
		searchLog.Warn("failed to parse search options", slog.Any("error", err))
		return toCString(encodeSearchError(stageParseOptions, err))
	}

	results, err := runSearch(opts)
	if err != nil {
		searchLog.Warn("search operation failed", slog.Any("error", err))
		return toCString(encodeSearchError(stageRunSearch, err))
	}

	resp, err := marshalJSON(SearchResponse{Results: results})
	if err != nil {
		searchLog.Error("failed to marshal search response", slog.Any("error", err))
		return toCString(encodeSearchError(stageMarshalPayload, err))
	}

	searchLog.Debug("native helm search request completed", slog.Int("resultCount", len(results)))

	return toCString(resp)
}

func recoverShowPanic(result **C.char, mode action.ShowOutputFormat, chartRef *C.char) {
	if recovered := recover(); recovered != nil {
		goChartRef := goString(chartRef)
		nativeLogger.Error(
			"panic recovered in helm show",
			slog.String("operation", "show"),
			slog.String("mode", mode.String()),
			slog.String("chartRef", goChartRef),
			slog.Any("panic", recovered),
			slog.String("stack", string(debug.Stack())),
		)
		*result = toCString(encodeShowError(mode, goChartRef, stagePanic, fmt.Errorf("panic: %v", recovered)))
	}
}

func recoverSearchPanic(result **C.char) {
	if recovered := recover(); recovered != nil {
		nativeLogger.Error(
			"panic recovered in helm search",
			slog.String("operation", "search"),
			slog.Any("panic", recovered),
			slog.String("stack", string(debug.Stack())),
		)
		*result = toCString(encodeSearchError(stagePanic, fmt.Errorf("panic: %v", recovered)))
	}
}

func dispatchShow(mode action.ShowOutputFormat, chartRef *C.char, options *C.char) *C.char {
	goChart := goString(chartRef)
	goOptions := goString(options)
	showLog := nativeLogger.With(
		slog.String("operation", "show"),
		slog.String("mode", mode.String()),
		slog.String("chartRef", goChart),
	)

	showLog.Debug("handling native helm show request")

	parsedOpts, err := parseShowOptions(goOptions)
	if err != nil {
		showLog.Warn("failed to parse show options", slog.Any("error", err))
		payload := encodeShowError(mode, goChart, stageParseOptions, err)
		return toCString(payload)
	}

	result, err := runShow(mode, goChart, parsedOpts)
	if err != nil {
		showLog.Warn("show operation failed", slog.Any("error", err))
		payload := encodeShowError(mode, goChart, stageRunShow, err)
		return toCString(payload)
	}

	showLog.Debug("native helm show request completed")

	return toCString(result)
}

func parseShowOptions(raw string) (ShowOptions, error) {
	return parseOptions[ShowOptions](raw)
}

func parseOptions[T any](raw string) (T, error) {
	var zero T
	if strings.TrimSpace(raw) == "" {
		return zero, nil
	}

	var options T
	if err := json.Unmarshal([]byte(raw), &options); err != nil {
		return zero, fmt.Errorf("decode options: %w", err)
	}

	return options, nil
}

func encodeShowError(mode action.ShowOutputFormat, chartRef string, stage string, err error) string {
	payload, marshalErr := marshalJSON(ShowError{
		Mode:     mode.String(),
		ChartRef: chartRef,
		Stage:    stage,
		Error:    err.Error(),
	})
	if marshalErr != nil {
		nativeLogger.Error(
			"failed to encode show error payload",
			slog.String("mode", mode.String()),
			slog.String("chartRef", chartRef),
			slog.String("stage", stage),
			slog.Any("error", marshalErr),
		)
		return `{"error":"failed to encode show error payload","stage":"` + stageMarshalError + `"}`
	}
	return payload
}

func encodeSearchError(stage string, err error) string {
	payload, marshalErr := marshalJSON(SearchError{Stage: stage, Error: err.Error()})
	if marshalErr != nil {
		nativeLogger.Error(
			"failed to encode search error payload",
			slog.String("stage", stage),
			slog.Any("error", marshalErr),
		)
		return `{"error":"failed to encode search error payload","stage":"` + stageMarshalError + `"}`
	}
	return payload
}

func main() {}
