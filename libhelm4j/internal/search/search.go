// Package search implements `helm search repo`. It builds a search index
// from the local repository cache and applies keyword, version, and
// semver-constraint filters.
package search

import (
	"errors"
	"fmt"
	"io/fs"
	"log/slog"
	"path/filepath"

	"github.com/Masterminds/semver/v3"
	helmSearch "helm.sh/helm/v4/pkg/cmd/search"
	"helm.sh/helm/v4/pkg/helmpath"
	"helm.sh/helm/v4/pkg/repo/v1"

	"github.com/thiagogcm/libhelm4j/internal/helmenv"
	"github.com/thiagogcm/libhelm4j/internal/helmlog"
)

const maxScore = 25

// ErrNoRepositoriesConfigured is returned when no repositories are configured.
var ErrNoRepositoriesConfigured = errors.New("no repositories configured")

// Options captures the options for helm search repo.
type Options struct {
	Keyword        string `json:"keyword,omitempty"`
	Regexp         bool   `json:"regexp,omitempty"`
	Versions       bool   `json:"versions,omitempty"`
	Devel          bool   `json:"devel,omitempty"`
	Version        string `json:"version,omitempty"`
	FailOnNoResult bool   `json:"failOnNoResult,omitempty"`
}

// Result represents a single search result.
type Result struct {
	Name        string `json:"name"`
	Version     string `json:"version"`
	AppVersion  string `json:"appVersion"`
	Description string `json:"description"`
	Score       int    `json:"score"`
}

// Response is the response for the search operation.
type Response struct {
	Results []Result `json:"results"`
}

// Run executes a helm search repo operation and returns the matching results.
func Run(opts Options) ([]Result, error) {
	log := helmlog.Logger()

	log.Debug(
		"running helm search",
		slog.Bool("regexp", opts.Regexp),
		slog.Bool("versions", opts.Versions),
		slog.Bool("devel", opts.Devel),
		slog.Bool("failOnNoResult", opts.FailOnNoResult),
	)

	env, err := helmenv.New()
	if err != nil {
		return nil, fmt.Errorf("bootstrap helm: %w", err)
	}

	i, err := buildIndex(
		env.Settings.RepositoryConfig,
		env.Settings.RepositoryCache,
		opts.Versions || opts.Version != "",
	)
	if err != nil {
		return nil, err
	}

	var res []*helmSearch.Result
	if opts.Keyword == "" {
		res = i.All()
	} else {
		res, err = i.Search(opts.Keyword, maxScore, opts.Regexp)
		if err != nil {
			return nil, fmt.Errorf("search index: %w", err)
		}
	}

	helmSearch.SortScore(res)

	// Apply version constraints.
	if opts.Version == "" {
		if opts.Devel {
			opts.Version = ">0.0.0-0"
		} else {
			opts.Version = ">0.0.0"
		}
	}

	constraint, err := semver.NewConstraint(opts.Version)
	if err != nil {
		return nil, fmt.Errorf("an invalid version/constraint format: %w", err)
	}

	var filtered []*helmSearch.Result
	foundNames := map[string]struct{}{}
	for _, r := range res {
		if !opts.Versions {
			if _, seen := foundNames[r.Name]; seen {
				continue
			}
		}

		if r.Chart == nil {
			continue
		}

		v, err := semver.NewVersion(r.Chart.Version)
		if err != nil {
			log.Debug(
				"skipping chart with invalid version",
				slog.String("name", r.Name),
				slog.String("version", r.Chart.Version),
				slog.Any("error", err),
			)
			continue
		}
		if constraint.Check(v) {
			filtered = append(filtered, r)
			foundNames[r.Name] = struct{}{}
		}
	}

	if len(filtered) == 0 && opts.FailOnNoResult {
		return nil, errors.New("no results found")
	}

	results := make([]Result, len(filtered))
	for idx, r := range filtered {
		results[idx] = Result{
			Name:        r.Name,
			Version:     r.Chart.Version,
			AppVersion:  r.Chart.AppVersion,
			Description: r.Chart.Description,
			Score:       r.Score,
		}
	}

	log.Debug("helm search completed", slog.Int("resultCount", len(results)))

	return results, nil
}

func buildIndex(repoFile, repoCacheDir string, includeAllVersions bool) (*helmSearch.Index, error) {
	log := helmlog.Logger()

	rf, err := repo.LoadFile(repoFile)
	if err != nil {
		if errors.Is(err, fs.ErrNotExist) {
			log.Debug("repository configuration missing", slog.String("repoFile", repoFile))
			return nil, ErrNoRepositoriesConfigured
		}
		return nil, fmt.Errorf("load repository config %q: %w", repoFile, err)
	}

	if len(rf.Repositories) == 0 {
		log.Debug("repository configuration is empty", slog.String("repoFile", repoFile))
		return nil, ErrNoRepositoriesConfigured
	}

	i := helmSearch.NewIndex()
	for _, repository := range rf.Repositories {
		repoName := repository.Name
		indexFile := filepath.Join(repoCacheDir, helmpath.CacheIndexFile(repoName))
		ind, err := repo.LoadIndexFile(indexFile)
		if err != nil {
			if errors.Is(err, fs.ErrNotExist) {
				log.Warn(
					"repository index not found; skipping repository",
					slog.String("repository", repoName),
					slog.String("indexFile", indexFile),
				)
			} else {
				log.Warn(
					"failed to load repository index; skipping repository",
					slog.String("repository", repoName),
					slog.String("indexFile", indexFile),
					slog.Any("error", err),
				)
			}
			continue
		}

		i.AddRepo(repoName, ind, includeAllVersions)
	}

	return i, nil
}
