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
	"strings"
	"unsafe"

	"helm.sh/helm/v4/pkg/action"

	"github.com/thiagogcm/libhelm4j/internal/bridge"
	"github.com/thiagogcm/libhelm4j/internal/helmlog"
	"github.com/thiagogcm/libhelm4j/internal/install"
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

//export HelmShow
func HelmShow(mode *C.char, chartRef *C.char, options *C.char) (result *C.char) {
	goMode := goString(mode)
	defer recoverShowPanic(&result, goMode, chartRef)

	showMode, err := parseShowMode(goMode)
	if err != nil {
		return toCString(bridge.EncodeError(bridge.StageRun, err, "mode", goMode, "chartRef", goString(chartRef)))
	}

	return dispatchShow(showMode, chartRef, options)
}

func parseShowMode(rawMode string) (action.ShowOutputFormat, error) {
	switch strings.ToLower(strings.TrimSpace(rawMode)) {
	case action.ShowChart.String():
		return action.ShowChart, nil
	case action.ShowValues.String():
		return action.ShowValues, nil
	case action.ShowReadme.String():
		return action.ShowReadme, nil
	case action.ShowAll.String():
		return action.ShowAll, nil
	case action.ShowCRDs.String():
		return action.ShowCRDs, nil
	case "":
		return action.ShowOutputFormat(""), fmt.Errorf("show mode is required")
	default:
		return action.ShowOutputFormat(""), fmt.Errorf("unsupported show mode: %s", rawMode)
	}
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
// Install export
// ---------------------------------------------------------------------------

//export HelmInstall
func HelmInstall(releaseName *C.char, chartRef *C.char, options *C.char) (result *C.char) {
	defer recoverInstallPanic(&result, releaseName, chartRef)

	goReleaseName := goString(releaseName)
	goChartRef := goString(chartRef)
	goOptions := goString(options)

	opts, err := bridge.ParseOptions[install.Options](goOptions)
	if err != nil {
		return toCString(bridge.EncodeError(bridge.StageParseOptions, err, "releaseName", goReleaseName, "chartRef", goChartRef))
	}

	res, err := install.Run(goReleaseName, goChartRef, opts)
	if err != nil {
		return toCString(bridge.EncodeError(bridge.StageRun, err, "releaseName", goReleaseName, "chartRef", goChartRef))
	}

	return toCString(res)
}

// ---------------------------------------------------------------------------
// Search export
// ---------------------------------------------------------------------------

//export HelmSearch
func HelmSearch(mode *C.char, options *C.char) (result *C.char) {
	goMode := goString(mode)
	defer recoverSearchPanic(&result, goMode)

	searchMode, err := search.ParseMode(goMode)
	if err != nil {
		return toCString(bridge.EncodeError(bridge.StageRun, err, "mode", goMode))
	}

	goOptions := goString(options)
	opts, err := bridge.ParseOptions[search.Options](goOptions)
	if err != nil {
		return toCString(bridge.EncodeError(bridge.StageParseOptions, err, "mode", searchMode.String()))
	}

	results, err := search.Run(searchMode, opts)
	if err != nil {
		return toCString(bridge.EncodeError(bridge.StageRun, err, "mode", searchMode.String()))
	}

	resp, err := bridge.MarshalJSON(search.Response{Mode: searchMode.String(), Results: results})
	if err != nil {
		return toCString(bridge.EncodeError(bridge.StageMarshal, err, "mode", searchMode.String()))
	}

	return toCString(resp)
}

// ---------------------------------------------------------------------------
// Repo exports
// ---------------------------------------------------------------------------

//export HelmRepo
func HelmRepo(mode *C.char, options *C.char) (result *C.char) {
	goMode := normalizeRepoMode(goString(mode))
	defer recoverRepoPanic(&result, goMode)
	return dispatchRepo(goMode, options)
}

func normalizeRepoMode(rawMode string) string {
	return strings.ToLower(strings.TrimSpace(rawMode))
}

// dispatchRepo routes the repo mode to the matching repomgr operation.
func dispatchRepo(mode string, options *C.char) *C.char {
	goOptions := goString(options)

	result, err := runRepoMode(mode, goOptions)
	if err != nil {
		return toCString(bridge.EncodeError(bridge.StageRun, err, "operation", repoOperation(mode)))
	}

	return toCString(result)
}

func runRepoMode(mode string, rawOptions string) (string, error) {
	switch mode {
	case "add":
		opts, err := bridge.ParseOptions[repomgr.AddOptions](rawOptions)
		if err != nil {
			return "", err
		}
		res, err := repomgr.Add(opts)
		if err != nil {
			return "", err
		}
		return bridge.MarshalJSON(res)
	case "update":
		opts, err := bridge.ParseOptions[repomgr.UpdateOptions](rawOptions)
		if err != nil {
			return "", err
		}
		res, err := repomgr.Update(opts)
		if err != nil {
			return "", err
		}
		return bridge.MarshalJSON(res)
	case "list":
		opts, err := bridge.ParseOptions[repomgr.ListOptions](rawOptions)
		if err != nil {
			return "", err
		}
		res, err := repomgr.List(opts)
		if err != nil {
			return "", err
		}
		return bridge.MarshalJSON(res)
	case "remove":
		opts, err := bridge.ParseOptions[repomgr.RemoveOptions](rawOptions)
		if err != nil {
			return "", err
		}
		res, err := repomgr.Remove(opts)
		if err != nil {
			return "", err
		}
		return bridge.MarshalJSON(res)
	case "":
		return "", fmt.Errorf("repo mode is required")
	default:
		return "", fmt.Errorf("unsupported repo mode: %s", mode)
	}
}

func repoOperation(mode string) string {
	if strings.TrimSpace(mode) == "" {
		return "repo"
	}
	return "repo " + mode
}

// ---------------------------------------------------------------------------
// Panic recovery
// ---------------------------------------------------------------------------

func recoverShowPanic(result **C.char, mode string, chartRef *C.char) {
	if recovered := recover(); recovered != nil {
		goChartRef := goString(chartRef)
		helmlog.Logger().Error(
			"panic recovered in helm show",
			slog.String("mode", mode),
			slog.String("chartRef", goChartRef),
			slog.Any("panic", recovered),
			slog.String("stack", string(debug.Stack())),
		)
		*result = toCString(bridge.EncodeError(bridge.StagePanic, fmt.Errorf("panic: %v", recovered), "mode", mode, "chartRef", goChartRef))
	}
}

func recoverInstallPanic(result **C.char, releaseName *C.char, chartRef *C.char) {
	if recovered := recover(); recovered != nil {
		goReleaseName := goString(releaseName)
		goChartRef := goString(chartRef)
		helmlog.Logger().Error(
			"panic recovered in helm install",
			slog.String("releaseName", goReleaseName),
			slog.String("chartRef", goChartRef),
			slog.Any("panic", recovered),
			slog.String("stack", string(debug.Stack())),
		)
		*result = toCString(bridge.EncodeError(bridge.StagePanic, fmt.Errorf("panic: %v", recovered), "releaseName", goReleaseName, "chartRef", goChartRef))
	}
}

func recoverSearchPanic(result **C.char, mode string) {
	if recovered := recover(); recovered != nil {
		helmlog.Logger().Error(
			"panic recovered in helm search",
			slog.String("mode", mode),
			slog.Any("panic", recovered),
			slog.String("stack", string(debug.Stack())),
		)
		*result = toCString(
			bridge.EncodeError(bridge.StagePanic, fmt.Errorf("panic: %v", recovered), "mode", mode))
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
		*result = toCString(bridge.EncodeError(bridge.StagePanic, fmt.Errorf("panic: %v", recovered), "operation", repoOperation(op)))
	}
}

func main() {}
