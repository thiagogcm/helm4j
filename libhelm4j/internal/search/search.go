// Package search implements `helm search <mode>` operations.
package search

import (
	"errors"
	"fmt"
	"io/fs"
	"log/slog"
	"strings"

	"github.com/Masterminds/semver/v3"
	"helm.sh/helm/v4/pkg/cmd/search"
	"helm.sh/helm/v4/pkg/helmpath"
	"helm.sh/helm/v4/pkg/repo/v1"

	"github.com/thiagogcm/libhelm4j/internal/helmenv"
	"github.com/thiagogcm/libhelm4j/internal/helmlog"
)

// searchMaxScore is the threshold passed to the upstream search.Index.
// The upstream uses lower-is-better scoring; anything below this threshold
// is considered a match.
const searchMaxScore = 25

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

	// Build a search index using the upstream search package.
	rf, repoURLByName, err := loadRepoIndex(env.Settings.RepositoryConfig)
	if err != nil {
		return nil, err
	}

	allVersions := opts.Versions || opts.Version != ""
	idx := search.NewIndex()
	for _, repository := range rf.Repositories {
		indexFile := helmpath.CacheIndexFile(repository.Name)
		ind, err := repo.LoadIndexFile(env.Settings.RepositoryCache + "/" + indexFile)
		if err != nil {
			if errors.Is(err, fs.ErrNotExist) {
				log.Warn("repository index not found; skipping repository",
					slog.String("repository", repository.Name))
			} else {
				log.Warn("failed to load repository index; skipping repository",
					slog.String("repository", repository.Name),
					slog.Any("error", err))
			}
			continue
		}
		idx.AddRepo(repository.Name, ind, allVersions)
	}

	// Search or return all entries.
	var matched []*search.Result
	if opts.Keyword == "" {
		matched = idx.All()
	} else {
		matched, err = idx.Search(opts.Keyword, searchMaxScore, opts.Regexp)
		if err != nil {
			return nil, fmt.Errorf("search index: %w", err)
		}
	}

	search.SortScore(matched)

	// Apply version constraints.
	versionExpr := opts.Version
	if versionExpr == "" {
		if opts.Devel {
			versionExpr = ">0.0.0-0"
		} else {
			versionExpr = ">0.0.0"
		}
	}

	constraint, err := semver.NewConstraint(versionExpr)
	if err != nil {
		return nil, fmt.Errorf("an invalid version/constraint format: %w", err)
	}

	var filtered []*search.Result
	foundNames := map[string]struct{}{}
	for _, r := range matched {
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
			log.Debug("skipping chart with invalid version",
				slog.String("name", r.Name),
				slog.String("version", r.Chart.Version),
				slog.Any("error", err))
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
	for i, r := range filtered {
		repoName := ""
		if parts := strings.SplitN(r.Name, "/", 2); len(parts) == 2 {
			repoName = parts[0]
		}
		results[i] = Result{
			Name:           r.Name,
			Version:        r.Chart.Version,
			AppVersion:     r.Chart.AppVersion,
			Description:    r.Chart.Description,
			Score:          r.Score,
			RepositoryName: repoName,
			RepositoryURL:  repoURLByName[repoName],
		}
	}

	log.Debug("helm search completed", slog.Int("resultCount", len(results)))

	return results, nil
}

// loadRepoIndex loads the repository file and returns the repo file plus a
// name→URL map for enriching search results.
func loadRepoIndex(repoFile string) (*repo.File, map[string]string, error) {
	log := helmlog.Logger()

	rf, err := repo.LoadFile(repoFile)
	if err != nil {
		if errors.Is(err, fs.ErrNotExist) {
			log.Debug("repository configuration missing", slog.String("repoFile", repoFile))
			return nil, nil, ErrNoRepositoriesConfigured
		}
		return nil, nil, fmt.Errorf("load repository config %q: %w", repoFile, err)
	}

	if len(rf.Repositories) == 0 {
		log.Debug("repository configuration is empty", slog.String("repoFile", repoFile))
		return nil, nil, ErrNoRepositoriesConfigured
	}

	urlByName := make(map[string]string, len(rf.Repositories))
	for _, r := range rf.Repositories {
		urlByName[r.Name] = r.URL
	}

	return rf, urlByName, nil
}

func normalizeVersion(raw string) *semver.Version {
	v, err := semver.NewVersion(raw)
	if err != nil {
		return nil
	}
	return v
}
