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
	"github.com/thiagogcm/libhelm4j/internal/dependency"
	"github.com/thiagogcm/libhelm4j/internal/getrelease"
	"github.com/thiagogcm/libhelm4j/internal/helmlog"
	"github.com/thiagogcm/libhelm4j/internal/history"
	"github.com/thiagogcm/libhelm4j/internal/install"
	"github.com/thiagogcm/libhelm4j/internal/lint"
	"github.com/thiagogcm/libhelm4j/internal/list"
	"github.com/thiagogcm/libhelm4j/internal/pkg"
	"github.com/thiagogcm/libhelm4j/internal/pull"
	"github.com/thiagogcm/libhelm4j/internal/push"
	"github.com/thiagogcm/libhelm4j/internal/registry"
	"github.com/thiagogcm/libhelm4j/internal/repomgr"
	"github.com/thiagogcm/libhelm4j/internal/rollback"
	"github.com/thiagogcm/libhelm4j/internal/search"
	"github.com/thiagogcm/libhelm4j/internal/show"
	"github.com/thiagogcm/libhelm4j/internal/status"
	"github.com/thiagogcm/libhelm4j/internal/template"
	"github.com/thiagogcm/libhelm4j/internal/test"
	"github.com/thiagogcm/libhelm4j/internal/uninstall"
	"github.com/thiagogcm/libhelm4j/internal/upgrade"
	"github.com/thiagogcm/libhelm4j/internal/version"
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
	defer recoverPanic(&result, "helm show", "mode", goMode, "chartRef", goString(chartRef))

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
	defer recoverPanic(&result, "helm install", "releaseName", goString(releaseName), "chartRef", goString(chartRef))

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
	defer recoverPanic(&result, "helm search", "mode", goMode)

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
	defer recoverPanic(&result, "helm repo", "operation", repoOperation(goMode))
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

func recoverPanic(result **C.char, operation string, kvPairs ...string) {
	if recovered := recover(); recovered != nil {
		helmlog.Logger().Error(
			"panic recovered in "+operation,
			slog.Any("panic", recovered),
			slog.String("stack", string(debug.Stack())),
		)
		*result = toCString(bridge.EncodeError(bridge.StagePanic, fmt.Errorf("panic: %v", recovered), kvPairs...))
	}
}

// ---------------------------------------------------------------------------
// Template export
// ---------------------------------------------------------------------------

//export HelmTemplate
func HelmTemplate(releaseName *C.char, chartRef *C.char, options *C.char) (result *C.char) {
	defer recoverPanic(&result, "helm template", "releaseName", goString(releaseName), "chartRef", goString(chartRef))

	goReleaseName := goString(releaseName)
	goChartRef := goString(chartRef)
	goOptions := goString(options)

	opts, err := bridge.ParseOptions[template.Options](goOptions)
	if err != nil {
		return toCString(bridge.EncodeError(bridge.StageParseOptions, err, "releaseName", goReleaseName, "chartRef", goChartRef))
	}

	res, err := template.Run(goReleaseName, goChartRef, opts)
	if err != nil {
		return toCString(bridge.EncodeError(bridge.StageRun, err, "releaseName", goReleaseName, "chartRef", goChartRef))
	}

	return toCString(res)
}

// ---------------------------------------------------------------------------
// Lint export
// ---------------------------------------------------------------------------

//export HelmLint
func HelmLint(chartPath *C.char, options *C.char) (result *C.char) {
	goChartPath := goString(chartPath)
	defer recoverPanic(&result, "helm lint", "chartPath", goChartPath)

	goOptions := goString(options)

	opts, err := bridge.ParseOptions[lint.Options](goOptions)
	if err != nil {
		return toCString(bridge.EncodeError(bridge.StageParseOptions, err, "chartPath", goChartPath))
	}

	res, err := lint.Run(goChartPath, opts)
	if err != nil {
		return toCString(bridge.EncodeError(bridge.StageRun, err, "chartPath", goChartPath))
	}

	return toCString(res)
}

// ---------------------------------------------------------------------------
// Version export
// ---------------------------------------------------------------------------

//export HelmVersion
func HelmVersion() (result *C.char) {
	defer recoverPanic(&result, "helm version")

	res, err := version.Run()
	if err != nil {
		return toCString(bridge.EncodeError(bridge.StageRun, err))
	}

	return toCString(res)
}

// ---------------------------------------------------------------------------
// Upgrade export
// ---------------------------------------------------------------------------

//export HelmUpgrade
func HelmUpgrade(releaseName *C.char, chartRef *C.char, options *C.char) (result *C.char) {
	defer recoverPanic(&result, "helm upgrade", "releaseName", goString(releaseName), "chartRef", goString(chartRef))

	goReleaseName := goString(releaseName)
	goChartRef := goString(chartRef)
	goOptions := goString(options)

	opts, err := bridge.ParseOptions[upgrade.Options](goOptions)
	if err != nil {
		return toCString(bridge.EncodeError(bridge.StageParseOptions, err, "releaseName", goReleaseName, "chartRef", goChartRef))
	}

	res, err := upgrade.Run(goReleaseName, goChartRef, opts)
	if err != nil {
		return toCString(bridge.EncodeError(bridge.StageRun, err, "releaseName", goReleaseName, "chartRef", goChartRef))
	}

	return toCString(res)
}

// ---------------------------------------------------------------------------
// Uninstall export
// ---------------------------------------------------------------------------

//export HelmUninstall
func HelmUninstall(releaseName *C.char, options *C.char) (result *C.char) {
	goReleaseName := goString(releaseName)
	defer recoverPanic(&result, "helm uninstall", "releaseName", goReleaseName)

	goOptions := goString(options)

	opts, err := bridge.ParseOptions[uninstall.Options](goOptions)
	if err != nil {
		return toCString(bridge.EncodeError(bridge.StageParseOptions, err, "releaseName", goReleaseName))
	}

	res, err := uninstall.Run(goReleaseName, opts)
	if err != nil {
		return toCString(bridge.EncodeError(bridge.StageRun, err, "releaseName", goReleaseName))
	}

	return toCString(res)
}

// ---------------------------------------------------------------------------
// Status export
// ---------------------------------------------------------------------------

//export HelmStatus
func HelmStatus(releaseName *C.char, options *C.char) (result *C.char) {
	goReleaseName := goString(releaseName)
	defer recoverPanic(&result, "helm status", "releaseName", goReleaseName)

	goOptions := goString(options)

	opts, err := bridge.ParseOptions[status.Options](goOptions)
	if err != nil {
		return toCString(bridge.EncodeError(bridge.StageParseOptions, err, "releaseName", goReleaseName))
	}

	res, err := status.Run(goReleaseName, opts)
	if err != nil {
		return toCString(bridge.EncodeError(bridge.StageRun, err, "releaseName", goReleaseName))
	}

	return toCString(res)
}

// ---------------------------------------------------------------------------
// Rollback export
// ---------------------------------------------------------------------------

//export HelmRollback
func HelmRollback(releaseName *C.char, options *C.char) (result *C.char) {
	goReleaseName := goString(releaseName)
	defer recoverPanic(&result, "helm rollback", "releaseName", goReleaseName)

	goOptions := goString(options)

	opts, err := bridge.ParseOptions[rollback.Options](goOptions)
	if err != nil {
		return toCString(bridge.EncodeError(bridge.StageParseOptions, err, "releaseName", goReleaseName))
	}

	res, err := rollback.Run(goReleaseName, opts)
	if err != nil {
		return toCString(bridge.EncodeError(bridge.StageRun, err, "releaseName", goReleaseName))
	}

	return toCString(res)
}

// ---------------------------------------------------------------------------
// History export
// ---------------------------------------------------------------------------

//export HelmHistory
func HelmHistory(releaseName *C.char, options *C.char) (result *C.char) {
	goReleaseName := goString(releaseName)
	defer recoverPanic(&result, "helm history", "releaseName", goReleaseName)

	goOptions := goString(options)

	opts, err := bridge.ParseOptions[history.Options](goOptions)
	if err != nil {
		return toCString(bridge.EncodeError(bridge.StageParseOptions, err, "releaseName", goReleaseName))
	}

	res, err := history.Run(goReleaseName, opts)
	if err != nil {
		return toCString(bridge.EncodeError(bridge.StageRun, err, "releaseName", goReleaseName))
	}

	return toCString(res)
}

// ---------------------------------------------------------------------------
// Get export
// ---------------------------------------------------------------------------

//export HelmGet
func HelmGet(mode *C.char, releaseName *C.char, options *C.char) (result *C.char) {
	goMode := goString(mode)
	goReleaseName := goString(releaseName)
	defer recoverPanic(&result, "helm get", "mode", goMode, "releaseName", goReleaseName)

	goOptions := goString(options)

	opts, err := bridge.ParseOptions[getrelease.Options](goOptions)
	if err != nil {
		return toCString(bridge.EncodeError(bridge.StageParseOptions, err, "mode", goMode, "releaseName", goReleaseName))
	}

	res, err := getrelease.Run(goMode, goReleaseName, opts)
	if err != nil {
		return toCString(bridge.EncodeError(bridge.StageRun, err, "mode", goMode, "releaseName", goReleaseName))
	}

	return toCString(res)
}

// ---------------------------------------------------------------------------
// List export
// ---------------------------------------------------------------------------

//export HelmList
func HelmList(options *C.char) (result *C.char) {
	defer recoverPanic(&result, "helm list")

	goOptions := goString(options)

	opts, err := bridge.ParseOptions[list.Options](goOptions)
	if err != nil {
		return toCString(bridge.EncodeError(bridge.StageParseOptions, err))
	}

	res, err := list.Run(opts)
	if err != nil {
		return toCString(bridge.EncodeError(bridge.StageRun, err))
	}

	return toCString(res)
}

// ---------------------------------------------------------------------------
// Pull export
// ---------------------------------------------------------------------------

//export HelmPull
func HelmPull(chartRef *C.char, options *C.char) (result *C.char) {
	goChartRef := goString(chartRef)
	defer recoverPanic(&result, "helm pull", "chartRef", goChartRef)

	goOptions := goString(options)

	opts, err := bridge.ParseOptions[pull.Options](goOptions)
	if err != nil {
		return toCString(bridge.EncodeError(bridge.StageParseOptions, err, "chartRef", goChartRef))
	}

	res, err := pull.Run(goChartRef, opts)
	if err != nil {
		return toCString(bridge.EncodeError(bridge.StageRun, err, "chartRef", goChartRef))
	}

	return toCString(res)
}

// ---------------------------------------------------------------------------
// Push export
// ---------------------------------------------------------------------------

//export HelmPush
func HelmPush(chartRef *C.char, remote *C.char, options *C.char) (result *C.char) {
	goChartRef := goString(chartRef)
	goRemote := goString(remote)
	defer recoverPanic(&result, "helm push", "chartRef", goChartRef, "remote", goRemote)

	goOptions := goString(options)

	opts, err := bridge.ParseOptions[push.Options](goOptions)
	if err != nil {
		return toCString(bridge.EncodeError(bridge.StageParseOptions, err, "chartRef", goChartRef, "remote", goRemote))
	}

	res, err := push.Run(goChartRef, goRemote, opts)
	if err != nil {
		return toCString(bridge.EncodeError(bridge.StageRun, err, "chartRef", goChartRef, "remote", goRemote))
	}

	return toCString(res)
}

// ---------------------------------------------------------------------------
// Package export
// ---------------------------------------------------------------------------

//export HelmPackage
func HelmPackage(chartPath *C.char, options *C.char) (result *C.char) {
	goChartPath := goString(chartPath)
	defer recoverPanic(&result, "helm package", "chartPath", goChartPath)

	goOptions := goString(options)

	opts, err := bridge.ParseOptions[pkg.Options](goOptions)
	if err != nil {
		return toCString(bridge.EncodeError(bridge.StageParseOptions, err, "chartPath", goChartPath))
	}

	res, err := pkg.Run(goChartPath, opts)
	if err != nil {
		return toCString(bridge.EncodeError(bridge.StageRun, err, "chartPath", goChartPath))
	}

	return toCString(res)
}

// ---------------------------------------------------------------------------
// Dependency export
// ---------------------------------------------------------------------------

//export HelmDependency
func HelmDependency(chartPath *C.char, options *C.char) (result *C.char) {
	goChartPath := goString(chartPath)
	defer recoverPanic(&result, "helm dependency", "chartPath", goChartPath)

	goOptions := goString(options)

	opts, err := bridge.ParseOptions[dependency.Options](goOptions)
	if err != nil {
		return toCString(bridge.EncodeError(bridge.StageParseOptions, err, "chartPath", goChartPath))
	}

	res, err := dependency.Run(goChartPath, opts)
	if err != nil {
		return toCString(bridge.EncodeError(bridge.StageRun, err, "chartPath", goChartPath))
	}

	return toCString(res)
}

// ---------------------------------------------------------------------------
// Registry export
// ---------------------------------------------------------------------------

//export HelmRegistry
func HelmRegistry(mode *C.char, hostname *C.char, options *C.char) (result *C.char) {
	goMode := goString(mode)
	goHostname := goString(hostname)
	defer recoverPanic(&result, "helm registry", "mode", goMode, "hostname", goHostname)

	goOptions := goString(options)

	opts, err := bridge.ParseOptions[registry.Options](goOptions)
	if err != nil {
		return toCString(bridge.EncodeError(bridge.StageParseOptions, err, "mode", goMode, "hostname", goHostname))
	}

	res, err := registry.Run(goMode, goHostname, opts)
	if err != nil {
		return toCString(bridge.EncodeError(bridge.StageRun, err, "mode", goMode, "hostname", goHostname))
	}

	return toCString(res)
}

// ---------------------------------------------------------------------------
// Test export
// ---------------------------------------------------------------------------

//export HelmTest
func HelmTest(releaseName *C.char, options *C.char) (result *C.char) {
	goReleaseName := goString(releaseName)
	defer recoverPanic(&result, "helm test", "releaseName", goReleaseName)

	goOptions := goString(options)

	opts, err := bridge.ParseOptions[test.Options](goOptions)
	if err != nil {
		return toCString(bridge.EncodeError(bridge.StageParseOptions, err, "releaseName", goReleaseName))
	}

	res, err := test.Run(goReleaseName, opts)
	if err != nil {
		return toCString(bridge.EncodeError(bridge.StageRun, err, "releaseName", goReleaseName))
	}

	return toCString(res)
}

func main() {}
