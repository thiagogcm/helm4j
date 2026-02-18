// Package search implements `helm search <mode>` operations.
package search

import (
	"errors"
	"fmt"
	"io/fs"
	"log/slog"
	"path/filepath"
	"regexp"
	"sort"
	"strings"

	"github.com/Masterminds/semver/v3"
	"helm.sh/helm/v4/pkg/helmpath"
	"helm.sh/helm/v4/pkg/repo/v1"

	"github.com/thiagogcm/libhelm4j/internal/helmenv"
	"github.com/thiagogcm/libhelm4j/internal/helmlog"
)

const maxScore = 25

// ErrNoRepositoriesConfigured is returned when no repositories are configured.
var ErrNoRepositoriesConfigured = errors.New("no repositories configured")

// Options captures the options for helm search operations.
type Options struct {
	Keyword        string `json:"keyword,omitempty"`
	Regexp         bool   `json:"regexp,omitempty"`
	Versions       bool   `json:"versions,omitempty"`
	Devel          bool   `json:"devel,omitempty"`
	Version        string `json:"version,omitempty"`
	FailOnNoResult bool   `json:"failOnNoResult,omitempty"`
	Endpoint       string `json:"endpoint,omitempty"`
	ListRepoURL    bool   `json:"listRepoUrl,omitempty"`
	MaxColWidth    int    `json:"maxColWidth,omitempty"`
}

// Result represents a single search result.
type Result struct {
	Name           string `json:"name"`
	Version        string `json:"version"`
	AppVersion     string `json:"appVersion"`
	Description    string `json:"description"`
	Score          int    `json:"score"`
	URL            string `json:"url,omitempty"`
	RepositoryName string `json:"repositoryName,omitempty"`
	RepositoryURL  string `json:"repositoryUrl,omitempty"`
}

// Response is the response for the search operation.
type Response struct {
	Mode    string   `json:"mode"`
	Results []Result `json:"results"`
}

type indexedChart struct {
	Name    string
	Repo    string
	RepoURL string
	Chart   *repo.ChartVersion
}

type scoredChart struct {
	Entry indexedChart
	Score int
}

func runRepo(opts Options) ([]Result, error) {
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

	entries, err := buildIndex(
		env.Settings.RepositoryConfig,
		env.Settings.RepositoryCache,
		opts.Versions || opts.Version != "",
	)
	if err != nil {
		return nil, err
	}

	matched, err := filterAndScore(entries, opts.Keyword, opts.Regexp)
	if err != nil {
		return nil, err
	}

	sort.SliceStable(matched, func(i, j int) bool {
		if matched[i].Score != matched[j].Score {
			return matched[i].Score > matched[j].Score
		}
		left := normalizeVersion(matched[i].Entry.Chart.Version)
		right := normalizeVersion(matched[j].Entry.Chart.Version)
		if left != nil && right != nil && !left.Equal(right) {
			return left.GreaterThan(right)
		}
		return matched[i].Entry.Name < matched[j].Entry.Name
	})

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

	var filtered []scoredChart
	foundNames := map[string]struct{}{}
	for _, candidate := range matched {
		r := candidate.Entry
		if !opts.Versions {
			if _, seen := foundNames[r.Name]; seen {
				continue
			}
		}

		if r.Chart == nil || r.Chart.Metadata == nil {
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
			filtered = append(filtered, candidate)
			foundNames[r.Name] = struct{}{}
		}
	}

	if len(filtered) == 0 && opts.FailOnNoResult {
		return nil, errors.New("no results found")
	}

	results := make([]Result, len(filtered))
	for idx, candidate := range filtered {
		r := candidate.Entry
		// r.Name has the form "repoName/chartName" — extract the repo name.
		repoName := ""
		if parts := strings.SplitN(r.Name, "/", 2); len(parts) == 2 {
			repoName = parts[0]
		}
		results[idx] = Result{
			Name:           r.Name,
			Version:        r.Chart.Version,
			AppVersion:     r.Chart.AppVersion,
			Description:    r.Chart.Description,
			Score:          candidate.Score,
			RepositoryName: repoName,
			RepositoryURL:  r.RepoURL,
		}
	}

	log.Debug("helm search completed", slog.Int("resultCount", len(results)))

	return results, nil
}

func buildIndex(repoFile, repoCacheDir string, includeAllVersions bool) ([]indexedChart, error) {
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

	charts := make([]indexedChart, 0)
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

		ind.SortEntries()
		for chartName, versions := range ind.Entries {
			if len(versions) == 0 {
				continue
			}

			selected := versions
			if !includeAllVersions {
				selected = versions[:1]
			}

			for _, cv := range selected {
				if cv == nil || cv.Metadata == nil {
					continue
				}
				charts = append(charts, indexedChart{
					Name:    repoName + "/" + chartName,
					Repo:    repoName,
					RepoURL: repository.URL,
					Chart:   cv,
				})
			}
		}
	}

	return charts, nil
}

func filterAndScore(entries []indexedChart, keyword string, useRegexp bool) ([]scoredChart, error) {
	if keyword == "" {
		results := make([]scoredChart, 0, len(entries))
		for _, entry := range entries {
			results = append(results, scoredChart{Entry: entry, Score: maxScore})
		}
		return results, nil
	}

	var matcher *regexp.Regexp
	if useRegexp {
		compiled, err := regexp.Compile(keyword)
		if err != nil {
			return nil, fmt.Errorf("search index: %w", err)
		}
		matcher = compiled
	}

	normalizedKeyword := strings.ToLower(keyword)
	results := make([]scoredChart, 0)
	for _, entry := range entries {
		description := ""
		if entry.Chart != nil {
			description = entry.Chart.Description
		}

		score := scoreMatch(entry.Name, description, normalizedKeyword, matcher)
		if score > 0 {
			results = append(results, scoredChart{Entry: entry, Score: score})
		}
	}

	return results, nil
}

func scoreMatch(name, description, keyword string, matcher *regexp.Regexp) int {
	if matcher != nil {
		if matcher.MatchString(name) {
			return maxScore
		}
		if matcher.MatchString(description) {
			return 18
		}
		return 0
	}

	normalizedName := strings.ToLower(name)
	normalizedDescription := strings.ToLower(description)

	switch {
	case normalizedName == keyword:
		return maxScore
	case strings.Contains(normalizedName, keyword):
		return 20
	case strings.Contains(normalizedDescription, keyword):
		return 12
	default:
		return 0
	}
}

func normalizeVersion(raw string) *semver.Version {
	v, err := semver.NewVersion(raw)
	if err != nil {
		return nil
	}
	return v
}
