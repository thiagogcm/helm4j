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
// Dispatch helpers
// ---------------------------------------------------------------------------

// dispatch parses options into T, runs op, and converts the result to a C
// string. A panic inside op is converted to an OperationError payload so the
// FFM boundary never sees a Go panic. kvPairs are forwarded to error and
// panic envelopes as context fields.
func dispatch[T any](operation string, options *C.char, op func(T) (string, error), kvPairs ...string) (result *C.char) {
	defer recoverPanic(&result, operation, kvPairs...)
	return toCString(bridge.Run(goString(options), op, kvPairs...))
}

// dispatchRaw hands op the raw JSON options string. Used by exports whose
// option type is selected from the request at runtime (e.g. HelmRepo).
func dispatchRaw(operation string, options *C.char, op func(string) (string, error), kvPairs ...string) (result *C.char) {
	defer recoverPanic(&result, operation, kvPairs...)
	res, err := op(goString(options))
	if err != nil {
		return toCString(bridge.EncodeError(bridge.StageRun, err, kvPairs...))
	}
	return toCString(res)
}

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
// Show export
// ---------------------------------------------------------------------------

//export HelmShow
func HelmShow(mode *C.char, chartRef *C.char, options *C.char) *C.char {
	goMode := goString(mode)
	goChart := goString(chartRef)

	showMode, err := parseShowMode(goMode)
	if err != nil {
		return toCString(bridge.EncodeError(bridge.StageRun, err, "mode", goMode, "chartRef", goChart))
	}

	return dispatch("helm show", options, func(o show.Options) (string, error) {
		return show.Run(showMode, goChart, o)
	}, "mode", showMode.String(), "chartRef", goChart)
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

// ---------------------------------------------------------------------------
// Install export
// ---------------------------------------------------------------------------

//export HelmInstall
func HelmInstall(releaseName *C.char, chartRef *C.char, options *C.char) *C.char {
	rn, cr := goString(releaseName), goString(chartRef)
	return dispatch("helm install", options, func(o install.Options) (string, error) {
		return install.Run(rn, cr, o)
	}, "releaseName", rn, "chartRef", cr)
}

// ---------------------------------------------------------------------------
// Search export
// ---------------------------------------------------------------------------

//export HelmSearch
func HelmSearch(mode *C.char, options *C.char) *C.char {
	goMode := goString(mode)

	searchMode, err := search.ParseMode(goMode)
	if err != nil {
		return toCString(bridge.EncodeError(bridge.StageRun, err, "mode", goMode))
	}

	return dispatch("helm search", options, func(o search.Options) (string, error) {
		results, err := search.Run(searchMode, o)
		if err != nil {
			return "", err
		}
		return bridge.MarshalJSON(search.Response{Mode: searchMode.String(), Results: results})
	}, "mode", searchMode.String())
}

// ---------------------------------------------------------------------------
// Repo exports
// ---------------------------------------------------------------------------

//export HelmRepo
func HelmRepo(mode *C.char, options *C.char) *C.char {
	repoMode := normalizeRepoMode(goString(mode))
	return dispatchRaw("helm repo", options, func(raw string) (string, error) {
		return runRepoMode(repoMode, raw)
	}, "operation", repoOperation(repoMode))
}

func normalizeRepoMode(rawMode string) string {
	return strings.ToLower(strings.TrimSpace(rawMode))
}

func runRepoMode(mode string, rawOptions string) (string, error) {
	switch mode {
	case "add":
		return runRepoOp[repomgr.AddOptions, repomgr.AddResponse](rawOptions, repomgr.Add)
	case "update":
		return runRepoOp[repomgr.UpdateOptions, repomgr.UpdateResponse](rawOptions, repomgr.Update)
	case "list":
		return runRepoOp[repomgr.ListOptions, repomgr.ListResponse](rawOptions, repomgr.List)
	case "remove":
		return runRepoOp[repomgr.RemoveOptions, repomgr.RemoveResponse](rawOptions, repomgr.Remove)
	case "":
		return "", fmt.Errorf("repo mode is required")
	default:
		return "", fmt.Errorf("unsupported repo mode: %s", mode)
	}
}

// runRepoOp parses raw into O, calls op, and JSON-encodes the response.
func runRepoOp[O any, R any](raw string, op func(O) (R, error)) (string, error) {
	opts, err := bridge.ParseOptions[O](raw)
	if err != nil {
		return "", err
	}
	res, err := op(opts)
	if err != nil {
		return "", err
	}
	return bridge.MarshalJSON(res)
}

func repoOperation(mode string) string {
	if strings.TrimSpace(mode) == "" {
		return "repo"
	}
	return "repo " + mode
}

// ---------------------------------------------------------------------------
// Template export
// ---------------------------------------------------------------------------

//export HelmTemplate
func HelmTemplate(releaseName *C.char, chartRef *C.char, options *C.char) *C.char {
	rn, cr := goString(releaseName), goString(chartRef)
	return dispatch("helm template", options, func(o template.Options) (string, error) {
		return template.Run(rn, cr, o)
	}, "releaseName", rn, "chartRef", cr)
}

// ---------------------------------------------------------------------------
// Lint export
// ---------------------------------------------------------------------------

//export HelmLint
func HelmLint(chartPath *C.char, options *C.char) *C.char {
	cp := goString(chartPath)
	return dispatch("helm lint", options, func(o lint.Options) (string, error) {
		return lint.Run(cp, o)
	}, "chartPath", cp)
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
func HelmUpgrade(releaseName *C.char, chartRef *C.char, options *C.char) *C.char {
	rn, cr := goString(releaseName), goString(chartRef)
	return dispatch("helm upgrade", options, func(o upgrade.Options) (string, error) {
		return upgrade.Run(rn, cr, o)
	}, "releaseName", rn, "chartRef", cr)
}

// ---------------------------------------------------------------------------
// Uninstall export
// ---------------------------------------------------------------------------

//export HelmUninstall
func HelmUninstall(releaseName *C.char, options *C.char) *C.char {
	rn := goString(releaseName)
	return dispatch("helm uninstall", options, func(o uninstall.Options) (string, error) {
		return uninstall.Run(rn, o)
	}, "releaseName", rn)
}

// ---------------------------------------------------------------------------
// Status export
// ---------------------------------------------------------------------------

//export HelmStatus
func HelmStatus(releaseName *C.char, options *C.char) *C.char {
	rn := goString(releaseName)
	return dispatch("helm status", options, func(o status.Options) (string, error) {
		return status.Run(rn, o)
	}, "releaseName", rn)
}

// ---------------------------------------------------------------------------
// Rollback export
// ---------------------------------------------------------------------------

//export HelmRollback
func HelmRollback(releaseName *C.char, options *C.char) *C.char {
	rn := goString(releaseName)
	return dispatch("helm rollback", options, func(o rollback.Options) (string, error) {
		return rollback.Run(rn, o)
	}, "releaseName", rn)
}

// ---------------------------------------------------------------------------
// History export
// ---------------------------------------------------------------------------

//export HelmHistory
func HelmHistory(releaseName *C.char, options *C.char) *C.char {
	rn := goString(releaseName)
	return dispatch("helm history", options, func(o history.Options) (string, error) {
		return history.Run(rn, o)
	}, "releaseName", rn)
}

// ---------------------------------------------------------------------------
// Get export
// ---------------------------------------------------------------------------

//export HelmGet
func HelmGet(mode *C.char, releaseName *C.char, options *C.char) *C.char {
	gm, rn := goString(mode), goString(releaseName)
	return dispatch("helm get", options, func(o getrelease.Options) (string, error) {
		return getrelease.Run(gm, rn, o)
	}, "mode", gm, "releaseName", rn)
}

// ---------------------------------------------------------------------------
// List export
// ---------------------------------------------------------------------------

//export HelmList
func HelmList(options *C.char) *C.char {
	return dispatch("helm list", options, list.Run)
}

// ---------------------------------------------------------------------------
// Pull export
// ---------------------------------------------------------------------------

//export HelmPull
func HelmPull(chartRef *C.char, options *C.char) *C.char {
	cr := goString(chartRef)
	return dispatch("helm pull", options, func(o pull.Options) (string, error) {
		return pull.Run(cr, o)
	}, "chartRef", cr)
}

// ---------------------------------------------------------------------------
// Push export
// ---------------------------------------------------------------------------

//export HelmPush
func HelmPush(chartRef *C.char, remote *C.char, options *C.char) *C.char {
	cr, rm := goString(chartRef), goString(remote)
	return dispatch("helm push", options, func(o push.Options) (string, error) {
		return push.Run(cr, rm, o)
	}, "chartRef", cr, "remote", rm)
}

// ---------------------------------------------------------------------------
// Package export
// ---------------------------------------------------------------------------

//export HelmPackage
func HelmPackage(chartPath *C.char, options *C.char) *C.char {
	cp := goString(chartPath)
	return dispatch("helm package", options, func(o pkg.Options) (string, error) {
		return pkg.Run(cp, o)
	}, "chartPath", cp)
}

// ---------------------------------------------------------------------------
// Dependency export
// ---------------------------------------------------------------------------

//export HelmDependency
func HelmDependency(chartPath *C.char, options *C.char) *C.char {
	cp := goString(chartPath)
	return dispatch("helm dependency", options, func(o dependency.Options) (string, error) {
		return dependency.Run(cp, o)
	}, "chartPath", cp)
}

// ---------------------------------------------------------------------------
// Registry export
// ---------------------------------------------------------------------------

//export HelmRegistry
func HelmRegistry(mode *C.char, hostname *C.char, options *C.char) *C.char {
	gm, hn := goString(mode), goString(hostname)
	return dispatch("helm registry", options, func(o registry.Options) (string, error) {
		return registry.Run(gm, hn, o)
	}, "mode", gm, "hostname", hn)
}

// ---------------------------------------------------------------------------
// Test export
// ---------------------------------------------------------------------------

//export HelmTest
func HelmTest(releaseName *C.char, options *C.char) *C.char {
	rn := goString(releaseName)
	return dispatch("helm test", options, func(o test.Options) (string, error) {
		return test.Run(rn, o)
	}, "releaseName", rn)
}

func main() {}
