// Package main is the CGo c-shared entry point for libhelm4j. It provides
// the thin dispatch layer between the FFM boundary (C char*) and the
// pure-Go internal packages that implement every Helm operation.
//
// Only //export functions, C-string helpers, panic recovery, and the
// required empty main() live here.
package main

/*
#include <stdlib.h>
*/
import "C"

import (
	"fmt"
	"log/slog"
	"runtime/debug"
	"unsafe"

	"helm.sh/helm/v4/pkg/action"

	"github.com/thiagogcm/libhelm4j/internal/bridge"
	"github.com/thiagogcm/libhelm4j/internal/helmlog"
	"github.com/thiagogcm/libhelm4j/internal/repomgr"
	"github.com/thiagogcm/libhelm4j/internal/search"
	"github.com/thiagogcm/libhelm4j/internal/show"
)

// ---------------------------------------------------------------------------
// C-string helpers — these must stay in package main because of cgo.
// ---------------------------------------------------------------------------

//export FreeString
func FreeString(str *C.char) {
	C.free(unsafe.Pointer(str))
}

func goString(cstr *C.char) string {
	if cstr == nil {
		return ""
	}
	return C.GoString(cstr)
}

func toCString(value string) *C.char {
	return C.CString(value)
}

// ---------------------------------------------------------------------------
// Show exports
// ---------------------------------------------------------------------------

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

func dispatchShow(mode action.ShowOutputFormat, chartRef *C.char, options *C.char) *C.char {
	goChart := goString(chartRef)
	goOptions := goString(options)

	opts, err := bridge.ParseOptions[show.Options](goOptions)
	if err != nil {
		return toCString(bridge.EncodeError(bridge.StageParseOptions, err, "mode", mode.String(), "chartRef", goChart))
	}

	result, err := show.Run(mode, goChart, opts)
	if err != nil {
		return toCString(bridge.EncodeError(bridge.StageRun, err, "mode", mode.String(), "chartRef", goChart))
	}

	return toCString(result)
}

// ---------------------------------------------------------------------------
// Search export
// ---------------------------------------------------------------------------

//export HelmSearch
func HelmSearch(options *C.char) (result *C.char) {
	defer recoverSearchPanic(&result)

	goOptions := goString(options)
	opts, err := bridge.ParseOptions[search.Options](goOptions)
	if err != nil {
		return toCString(bridge.EncodeError(bridge.StageParseOptions, err))
	}

	results, err := search.Run(opts)
	if err != nil {
		return toCString(bridge.EncodeError(bridge.StageRun, err))
	}

	resp, err := bridge.MarshalJSON(search.Response{Results: results})
	if err != nil {
		return toCString(bridge.EncodeError(bridge.StageMarshal, err))
	}

	return toCString(resp)
}

// ---------------------------------------------------------------------------
// Repo exports
// ---------------------------------------------------------------------------

//export HelmRepoAdd
func HelmRepoAdd(options *C.char) (result *C.char) {
	defer recoverRepoPanic(&result, "add")
	return dispatchRepo("add", options, func(raw string) (string, error) {
		opts, err := bridge.ParseOptions[repomgr.AddOptions](raw)
		if err != nil {
			return "", err
		}
		res, err := repomgr.Add(opts)
		if err != nil {
			return "", err
		}
		return bridge.MarshalJSON(res)
	})
}

//export HelmRepoUpdate
func HelmRepoUpdate(options *C.char) (result *C.char) {
	defer recoverRepoPanic(&result, "update")
	return dispatchRepo("update", options, func(raw string) (string, error) {
		opts, err := bridge.ParseOptions[repomgr.UpdateOptions](raw)
		if err != nil {
			return "", err
		}
		res, err := repomgr.Update(opts)
		if err != nil {
			return "", err
		}
		return bridge.MarshalJSON(res)
	})
}

//export HelmRepoList
func HelmRepoList(options *C.char) (result *C.char) {
	defer recoverRepoPanic(&result, "list")
	return dispatchRepo("list", options, func(raw string) (string, error) {
		opts, err := bridge.ParseOptions[repomgr.ListOptions](raw)
		if err != nil {
			return "", err
		}
		res, err := repomgr.List(opts)
		if err != nil {
			return "", err
		}
		return bridge.MarshalJSON(res)
	})
}

//export HelmRepoRemove
func HelmRepoRemove(options *C.char) (result *C.char) {
	defer recoverRepoPanic(&result, "remove")
	return dispatchRepo("remove", options, func(raw string) (string, error) {
		opts, err := bridge.ParseOptions[repomgr.RemoveOptions](raw)
		if err != nil {
			return "", err
		}
		res, err := repomgr.Remove(opts)
		if err != nil {
			return "", err
		}
		return bridge.MarshalJSON(res)
	})
}

// dispatchRepo is the common dispatch helper for all repo operations.
// The caller supplies a function that parses options, runs the operation,
// and marshals the result.
func dispatchRepo(op string, options *C.char, run func(string) (string, error)) *C.char {
	goOptions := goString(options)

	result, err := run(goOptions)
	if err != nil {
		return toCString(bridge.EncodeError(bridge.StageRun, err, "operation", "repo "+op))
	}

	return toCString(result)
}

// ---------------------------------------------------------------------------
// Panic recovery
// ---------------------------------------------------------------------------

func recoverShowPanic(result **C.char, mode action.ShowOutputFormat, chartRef *C.char) {
	if recovered := recover(); recovered != nil {
		goChartRef := goString(chartRef)
		helmlog.Logger().Error(
			"panic recovered in helm show",
			slog.String("mode", mode.String()),
			slog.String("chartRef", goChartRef),
			slog.Any("panic", recovered),
			slog.String("stack", string(debug.Stack())),
		)
		*result = toCString(bridge.EncodeError(bridge.StagePanic, fmt.Errorf("panic: %v", recovered), "mode", mode.String(), "chartRef", goChartRef))
	}
}

func recoverSearchPanic(result **C.char) {
	if recovered := recover(); recovered != nil {
		helmlog.Logger().Error(
			"panic recovered in helm search",
			slog.Any("panic", recovered),
			slog.String("stack", string(debug.Stack())),
		)
		*result = toCString(bridge.EncodeError(bridge.StagePanic, fmt.Errorf("panic: %v", recovered)))
	}
}

func recoverRepoPanic(result **C.char, op string) {
	if recovered := recover(); recovered != nil {
		helmlog.Logger().Error(
			"panic recovered in helm repo",
			slog.String("operation", op),
			slog.Any("panic", recovered),
			slog.String("stack", string(debug.Stack())),
		)
		*result = toCString(bridge.EncodeError(bridge.StagePanic, fmt.Errorf("panic: %v", recovered), "operation", "repo "+op))
	}
}

func main() {}
