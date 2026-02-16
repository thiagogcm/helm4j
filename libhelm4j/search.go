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

	repoFile := env.Settings.RepositoryConfig
	repoCacheDir := env.Settings.RepositoryCache

	rf, err := repo.LoadFile(repoFile)
	if err != nil {
		if errors.Is(err, fs.ErrNotExist) {
			nativeLogger.Debug(
				"repository configuration missing; returning empty search results",
				slog.String("repoFile", repoFile),
			)
			return []SearchResult{}, nil
		}
		return nil, fmt.Errorf("load repository config %q: %w", repoFile, err)
	}

	if len(rf.Repositories) == 0 {
		nativeLogger.Debug(
			"repository configuration is empty; returning empty search results",
			slog.String("repoFile", repoFile),
		)
		return []SearchResult{}, nil
	}

	i := search.NewIndex()
	for _, re := range rf.Repositories {
		n := re.Name
		f := filepath.Join(repoCacheDir, helmpath.CacheIndexFile(n))
		ind, err := repo.LoadIndexFile(f)
		if err != nil {
			if errors.Is(err, fs.ErrNotExist) {
				nativeLogger.Warn(
					"repository index not found; skipping repository",
					slog.String("repository", n),
					slog.String("indexFile", f),
				)
			} else {
				nativeLogger.Warn(
					"failed to load repository index; skipping repository",
					slog.String("repository", n),
					slog.String("indexFile", f),
					slog.Any("error", err),
				)
			}
			continue
		}
		i.AddRepo(n, ind, opts.Versions || len(opts.Version) > 0)
	}

	var res []*search.Result
	if opts.Keyword == "" {
		res = i.All()
	} else {
		res, err = i.Search(opts.Keyword, 25, opts.Regexp)
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
		return nil, fmt.Errorf("invalid version constraint: %w", err)
	}

	var filtered []*search.Result
	foundNames := map[string]bool{}
	for _, r := range res {
		if !opts.Versions && foundNames[r.Name] {
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
			foundNames[r.Name] = true
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
