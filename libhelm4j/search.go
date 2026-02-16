package main

import (
	"errors"
	"fmt"
	"io/fs"
	"log/slog"
	"path/filepath"

	"github.com/Masterminds/semver/v3"
	"helm.sh/helm/v4/pkg/cmd/search"
	"helm.sh/helm/v4/pkg/helmpath"
	"helm.sh/helm/v4/pkg/repo/v1"
)

const searchMaxScore = 25

var errNoRepositoriesConfigured = errors.New("no repositories configured")

// SearchOptions captures the options for helm search repo
type SearchOptions struct {
	Keyword        string `json:"keyword,omitempty"`
	Regexp         bool   `json:"regexp,omitempty"`
	Versions       bool   `json:"versions,omitempty"`
	Devel          bool   `json:"devel,omitempty"`
	Version        string `json:"version,omitempty"`
	FailOnNoResult bool   `json:"failOnNoResult,omitempty"`
}

// SearchResult represents a single search result
type SearchResult struct {
	Name        string `json:"name"`
	Version     string `json:"version"`
	AppVersion  string `json:"appVersion"`
	Description string `json:"description"`
	Score       int    `json:"score"`
}

// SearchResponse is the response for the search operation
type SearchResponse struct {
	Results []SearchResult `json:"results"`
}

func runSearch(opts SearchOptions) ([]SearchResult, error) {
	nativeLogger.Debug(
		"running helm search",
		slog.Bool("regexp", opts.Regexp),
		slog.Bool("versions", opts.Versions),
		slog.Bool("devel", opts.Devel),
		slog.Bool("failOnNoResult", opts.FailOnNoResult),
	)

	env, err := newHelmEnv()
	if err != nil {
		return nil, fmt.Errorf("bootstrap helm: %w", err)
	}

	i, err := buildSearchIndex(
		env.Settings.RepositoryConfig,
		env.Settings.RepositoryCache,
		opts.Versions || opts.Version != "",
	)
	if err != nil {
		return nil, err
	}

	var res []*search.Result
	if opts.Keyword == "" {
		res = i.All()
	} else {
		res, err = i.Search(opts.Keyword, searchMaxScore, opts.Regexp)
		if err != nil {
			return nil, fmt.Errorf("search index: %w", err)
		}
	}

	search.SortScore(res)

	// Apply version constraints
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

	var filtered []*search.Result
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
			nativeLogger.Debug(
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

	searchResults := make([]SearchResult, len(filtered))
	for idx, r := range filtered {
		searchResults[idx] = SearchResult{
			Name:        r.Name,
			Version:     r.Chart.Version,
			AppVersion:  r.Chart.AppVersion,
			Description: r.Chart.Description,
			Score:       r.Score,
		}
	}

	nativeLogger.Debug("helm search completed", slog.Int("resultCount", len(searchResults)))

	return searchResults, nil
}

func buildSearchIndex(repoFile, repoCacheDir string, includeAllVersions bool) (*search.Index, error) {
	rf, err := repo.LoadFile(repoFile)
	if err != nil {
		if errors.Is(err, fs.ErrNotExist) {
			nativeLogger.Debug(
				"repository configuration missing",
				slog.String("repoFile", repoFile),
			)
			return nil, errNoRepositoriesConfigured
		}
		return nil, fmt.Errorf("load repository config %q: %w", repoFile, err)
	}

	if len(rf.Repositories) == 0 {
		nativeLogger.Debug(
			"repository configuration is empty",
			slog.String("repoFile", repoFile),
		)
		return nil, errNoRepositoriesConfigured
	}

	i := search.NewIndex()
	for _, repository := range rf.Repositories {
		repositoryName := repository.Name
		indexFile := filepath.Join(repoCacheDir, helmpath.CacheIndexFile(repositoryName))
		ind, err := repo.LoadIndexFile(indexFile)
		if err != nil {
			if errors.Is(err, fs.ErrNotExist) {
				nativeLogger.Warn(
					"repository index not found; skipping repository",
					slog.String("repository", repositoryName),
					slog.String("indexFile", indexFile),
				)
			} else {
				nativeLogger.Warn(
					"failed to load repository index; skipping repository",
					slog.String("repository", repositoryName),
					slog.String("indexFile", indexFile),
					slog.Any("error", err),
				)
			}
			continue
		}

		i.AddRepo(repositoryName, ind, includeAllVersions)
	}

	return i, nil
}
