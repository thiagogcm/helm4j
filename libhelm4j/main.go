package main

/*
#include <stdlib.h>
*/
import "C"

import (
	"encoding/json"
	"fmt"
	"strings"
	"unsafe"

	"helm.sh/helm/v4/pkg/action"
)

//export FreeString
func FreeString(str *C.char) {
	C.free(unsafe.Pointer(str))
}

//export HelmShowChart
func HelmShowChart(chartRef *C.char, options *C.char) *C.char {
	return dispatchShow(action.ShowChart, chartRef, options)
}

//export HelmShowValues
func HelmShowValues(chartRef *C.char, options *C.char) *C.char {
	return dispatchShow(action.ShowValues, chartRef, options)
}

//export HelmShowReadme
func HelmShowReadme(chartRef *C.char, options *C.char) *C.char {
	return dispatchShow(action.ShowReadme, chartRef, options)
}

//export HelmShowAll
func HelmShowAll(chartRef *C.char, options *C.char) *C.char {
	return dispatchShow(action.ShowAll, chartRef, options)
}

//export HelmSearch
func HelmSearch(options *C.char) *C.char {
	goOptions := goString(options)
	opts, err := parseSearchOptions(goOptions)
	if err != nil {
		return toCString(encodeSearchError("parseOptions", err))
	}

	results, err := runSearch(opts)
	if err != nil {
		return toCString(encodeSearchError("runSearch", err))
	}

	resp, err := marshalJSON(SearchResponse{Results: results})
	if err != nil {
		return toCString(encodeSearchError("marshalResponse", err))
	}

	return toCString(resp)
}

func dispatchShow(mode action.ShowOutputFormat, chartRef *C.char, options *C.char) *C.char {
	goChart := goString(chartRef)
	goOptions := goString(options)

	parsedOpts, err := parseShowOptions(goOptions)
	if err != nil {
		payload := encodeShowError(mode, goChart, "parseOptions", err)
		return toCString(payload)
	}

	result, err := runShow(mode, goChart, parsedOpts)
	if err != nil {
		payload := encodeShowError(mode, goChart, "runShow", err)
		return toCString(payload)
	}

	return toCString(result)
}

func parseSearchOptions(raw string) (SearchOptions, error) {
	if strings.TrimSpace(raw) == "" {
		return SearchOptions{}, nil
	}

	var options SearchOptions
	if err := json.Unmarshal([]byte(raw), &options); err != nil {
		return SearchOptions{}, fmt.Errorf("decode options: %w", err)
	}

	return options, nil
}

func parseShowOptions(raw string) (ShowOptions, error) {
	if strings.TrimSpace(raw) == "" {
		return ShowOptions{}, nil
	}

	var options ShowOptions
	if err := json.Unmarshal([]byte(raw), &options); err != nil {
		return ShowOptions{}, fmt.Errorf("decode options: %w", err)
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
		return fmt.Sprintf(`{"error":"%s","stage":"%s","mode":"%s"}`, marshalErr.Error(), stage, mode)
	}
	return payload
}

func encodeSearchError(stage string, err error) string {
	return fmt.Sprintf(`{"error":"%s","stage":"%s"}`, err.Error(), stage)
}

func main() {}
