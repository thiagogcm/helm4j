// Package repomgr implements `helm repo add`, `helm repo update`,
// `helm repo list`, and `helm repo remove` — repository management
// operations used to configure chart sources before search or install.
package repomgr

import (
	"errors"
	"fmt"
	"io/fs"
	"log/slog"
	"os"
	"strings"
	"sync"
	"time"

	"helm.sh/helm/v4/pkg/getter"
	"helm.sh/helm/v4/pkg/helmpath"
	"helm.sh/helm/v4/pkg/repo/v1"

	"github.com/thiagogcm/libhelm4j/internal/helmenv"
	"github.com/thiagogcm/libhelm4j/internal/helmlog"
)

// Sentinel errors returned by the repomgr operations.
var (
	ErrNoRepositoriesConfigured = errors.New("no repositories configured")
	ErrRepositoryAlreadyExists  = errors.New("repository already exists")
	ErrRepositoryNotFound       = errors.New("repository not found")
)

// Repositories that have been permanently deleted and no longer work.
var deprecatedRepos = map[string]string{
	"//kubernetes-charts.storage.googleapis.com":           "https://charts.helm.sh/stable",
	"//kubernetes-charts-incubator.storage.googleapis.com": "https://charts.helm.sh/incubator",
}

// ---------------------------------------------------------------------------
// Options
// ---------------------------------------------------------------------------

// AddOptions captures the parameters for helm repo add.
type AddOptions struct {
	Name                  string `json:"name"`
	URL                   string `json:"url"`
	Username              string `json:"username,omitempty"`
	Password              string `json:"password,omitempty"`
	CertFile              string `json:"certFile,omitempty"`
	KeyFile               string `json:"keyFile,omitempty"`
	CaFile                string `json:"caFile,omitempty"`
	InsecureSkipTLSVerify bool   `json:"insecureSkipTlsVerify,omitempty"`
	PassCredentialsAll    bool   `json:"passCredentialsAll,omitempty"`
	ForceUpdate           bool   `json:"forceUpdate,omitempty"`
	AllowDeprecatedRepos  bool   `json:"allowDeprecatedRepos,omitempty"`
	Timeout               string `json:"timeout,omitempty"`
}

// UpdateOptions captures the parameters for helm repo update.
// An empty Names slice means "update all repositories".
type UpdateOptions struct {
	Names   []string `json:"names,omitempty"`
	Timeout string   `json:"timeout,omitempty"`
}

// ListOptions is intentionally empty; it exists so that every repomgr
// operation can use bridge.ParseOptions[T] consistently.
type ListOptions struct{}

// RemoveOptions captures the parameters for helm repo remove.
type RemoveOptions struct {
	Names []string `json:"names"`
}

// ---------------------------------------------------------------------------
// Result types
// ---------------------------------------------------------------------------

// AddResponse is returned on a successful repo add.
type AddResponse struct {
	Name string `json:"name"`
	URL  string `json:"url"`
}

// UpdateEntry reports the outcome of a single repo update.
type UpdateEntry struct {
	Name   string `json:"name"`
	Status string `json:"status"`
}

// UpdateResponse wraps the per-repo update results.
type UpdateResponse struct {
	Repositories []UpdateEntry `json:"repositories"`
}

// RepoEntry is a single repository in the list.
type RepoEntry struct {
	Name string `json:"name"`
	URL  string `json:"url"`
}

// ListResponse wraps the list of configured repositories.
type ListResponse struct {
	Repositories []RepoEntry `json:"repositories"`
}

// RemoveResponse lists the names that were removed.
type RemoveResponse struct {
	Removed []string `json:"removed"`
}

// ---------------------------------------------------------------------------
// Add
// ---------------------------------------------------------------------------

// Add registers a new chart repository, downloads its index to validate
// reachability, and persists the updated repository configuration.
func Add(opts AddOptions) (AddResponse, error) {
	log := helmlog.Logger()
	log.Debug(
		"running helm repo add",
		slog.String("name", opts.Name),
		slog.String("url", opts.URL),
		slog.Bool("forceUpdate", opts.ForceUpdate),
	)

	if opts.Name == "" {
		return AddResponse{}, errors.New("repository name is required")
	}
	if opts.URL == "" {
		return AddResponse{}, errors.New("repository URL is required")
	}
	if !opts.AllowDeprecatedRepos {
		for oldURL, replacementURL := range deprecatedRepos {
			if strings.Contains(opts.URL, oldURL) {
				return AddResponse{}, fmt.Errorf(
					"repo %q is no longer available; try %q instead",
					opts.URL,
					replacementURL,
				)
			}
		}
	}

	env, err := helmenv.New()
	if err != nil {
		return AddResponse{}, fmt.Errorf("bootstrap helm: %w", err)
	}

	repoFile, err := loadOrCreateRepoFile(env.Settings.RepositoryConfig)
	if err != nil {
		return AddResponse{}, err
	}

	if repoFile.Has(opts.Name) && !opts.ForceUpdate {
		return AddResponse{}, fmt.Errorf("%w: %s", ErrRepositoryAlreadyExists, opts.Name)
	}

	entry := &repo.Entry{
		Name:                  opts.Name,
		URL:                   opts.URL,
		Username:              opts.Username,
		Password:              opts.Password,
		CertFile:              opts.CertFile,
		KeyFile:               opts.KeyFile,
		CAFile:                opts.CaFile,
		InsecureSkipTLSVerify: opts.InsecureSkipTLSVerify,
		PassCredentialsAll:    opts.PassCredentialsAll,
	}

	chartRepo, err := repo.NewChartRepository(
		entry,
		getter.All(env.Settings, getter.WithTimeout(parseTimeout(opts.Timeout))),
	)
	if err != nil {
		return AddResponse{}, fmt.Errorf("create chart repository: %w", err)
	}
	chartRepo.CachePath = env.Settings.RepositoryCache

	if _, err := chartRepo.DownloadIndexFile(); err != nil {
		return AddResponse{}, fmt.Errorf("download index for %q: %w", opts.Name, err)
	}

	repoFile.Update(entry)

	if err := repoFile.WriteFile(env.Settings.RepositoryConfig, 0o600); err != nil {
		return AddResponse{}, fmt.Errorf("write repository config: %w", err)
	}

	log.Debug("helm repo add completed", slog.String("name", opts.Name))

	return AddResponse{Name: opts.Name, URL: opts.URL}, nil
}

// ---------------------------------------------------------------------------
// Update
// ---------------------------------------------------------------------------

// Update refreshes the local index cache for the requested repositories
// (or all of them when opts.Names is empty).
func Update(opts UpdateOptions) (UpdateResponse, error) {
	log := helmlog.Logger()
	log.Debug("running helm repo update", slog.Any("names", opts.Names))

	env, err := helmenv.New()
	if err != nil {
		return UpdateResponse{}, fmt.Errorf("bootstrap helm: %w", err)
	}

	repoFile, err := loadRepoFile(env.Settings.RepositoryConfig)
	if err != nil {
		return UpdateResponse{}, err
	}

	entries, err := selectEntries(repoFile, opts.Names)
	if err != nil {
		return UpdateResponse{}, err
	}

	getters := getter.All(env.Settings, getter.WithTimeout(parseTimeout(opts.Timeout)))
	results := make([]UpdateEntry, len(entries))

	var wg sync.WaitGroup
	for i, entry := range entries {
		wg.Go(func() {
			results[i] = updateOneRepo(entry, getters, env.Settings.RepositoryCache)
		})
	}
	wg.Wait()

	log.Debug("helm repo update completed", slog.Int("count", len(results)))

	return UpdateResponse{Repositories: results}, nil
}

// ---------------------------------------------------------------------------
// List
// ---------------------------------------------------------------------------

// List returns every configured chart repository.
func List(_ ListOptions) (ListResponse, error) {
	log := helmlog.Logger()
	log.Debug("running helm repo list")

	env, err := helmenv.New()
	if err != nil {
		return ListResponse{}, fmt.Errorf("bootstrap helm: %w", err)
	}

	repoFile, err := loadRepoFile(env.Settings.RepositoryConfig)
	if err != nil {
		return ListResponse{}, err
	}

	entries := make([]RepoEntry, 0, len(repoFile.Repositories))
	for _, r := range repoFile.Repositories {
		entries = append(entries, RepoEntry{Name: r.Name, URL: r.URL})
	}

	log.Debug("helm repo list completed", slog.Int("count", len(entries)))

	return ListResponse{Repositories: entries}, nil
}

// ---------------------------------------------------------------------------
// Remove
// ---------------------------------------------------------------------------

// Remove deletes one or more repositories from the configuration and
// cleans up their cached index and chart-list files.
func Remove(opts RemoveOptions) (RemoveResponse, error) {
	log := helmlog.Logger()
	log.Debug("running helm repo remove", slog.Any("names", opts.Names))

	if len(opts.Names) == 0 {
		return RemoveResponse{}, errors.New("at least one repository name is required")
	}

	env, err := helmenv.New()
	if err != nil {
		return RemoveResponse{}, fmt.Errorf("bootstrap helm: %w", err)
	}

	repoFile, err := loadRepoFile(env.Settings.RepositoryConfig)
	if err != nil {
		return RemoveResponse{}, err
	}

	// Validate all names exist before removing any.
	for _, name := range opts.Names {
		if !repoFile.Has(name) {
			return RemoveResponse{}, fmt.Errorf("%w: %s", ErrRepositoryNotFound, name)
		}
	}

	removed := make([]string, 0, len(opts.Names))
	for _, name := range opts.Names {
		repoFile.Remove(name)
		removeCachedFiles(env.Settings.RepositoryCache, name, log)
		removed = append(removed, name)
	}

	if err := repoFile.WriteFile(env.Settings.RepositoryConfig, 0o600); err != nil {
		return RemoveResponse{}, fmt.Errorf("write repository config: %w", err)
	}

	log.Debug("helm repo remove completed", slog.Int("count", len(removed)))

	return RemoveResponse{Removed: removed}, nil
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

// updateOneRepo downloads the index for a single repository entry. It is
// designed to be called concurrently from Update via sync.WaitGroup.Go.
func updateOneRepo(entry *repo.Entry, getters getter.Providers, cacheDir string) UpdateEntry {
	chartRepo, err := repo.NewChartRepository(entry, getters)
	if err != nil {
		return UpdateEntry{
			Name:   entry.Name,
			Status: fmt.Sprintf("error: %v", err),
		}
	}
	chartRepo.CachePath = cacheDir

	if _, err := chartRepo.DownloadIndexFile(); err != nil {
		return UpdateEntry{
			Name:   entry.Name,
			Status: fmt.Sprintf("error: %v", err),
		}
	}

	return UpdateEntry{Name: entry.Name, Status: "ok"}
}

// loadRepoFile loads the repository configuration file. It returns
// ErrNoRepositoriesConfigured when the file is missing or contains no
// entries.
func loadRepoFile(path string) (*repo.File, error) {
	rf, err := repo.LoadFile(path)
	if err != nil {
		if errors.Is(err, fs.ErrNotExist) {
			return nil, ErrNoRepositoriesConfigured
		}
		return nil, fmt.Errorf("load repository config %q: %w", path, err)
	}
	if len(rf.Repositories) == 0 {
		return nil, ErrNoRepositoriesConfigured
	}
	return rf, nil
}

// loadOrCreateRepoFile loads the repository file when it exists; otherwise
// it returns a fresh, empty File.
func loadOrCreateRepoFile(path string) (*repo.File, error) {
	rf, err := repo.LoadFile(path)
	if err != nil {
		if errors.Is(err, fs.ErrNotExist) {
			return repo.NewFile(), nil
		}
		return nil, fmt.Errorf("load repository config %q: %w", path, err)
	}
	return rf, nil
}

// selectEntries returns the subset of repository entries matching names.
// An empty names slice returns all entries. An unknown name returns an error.
func selectEntries(rf *repo.File, names []string) ([]*repo.Entry, error) {
	if len(names) == 0 {
		return rf.Repositories, nil
	}
	selected := make([]*repo.Entry, 0, len(names))
	for _, name := range names {
		entry := rf.Get(name)
		if entry == nil {
			return nil, fmt.Errorf("%w: %s", ErrRepositoryNotFound, name)
		}
		selected = append(selected, entry)
	}
	return selected, nil
}

func parseTimeout(rawTimeout string) time.Duration {
	timeout := strings.TrimSpace(rawTimeout)
	if timeout == "" {
		return getter.DefaultHTTPTimeout * time.Second
	}
	parsed, err := time.ParseDuration(timeout)
	if err != nil {
		return getter.DefaultHTTPTimeout * time.Second
	}
	return parsed
}

// removeCachedFiles deletes the index and chart-list files for the named
// repository from the cache directory. Uses os.OpenRoot to confine file
// operations within the cache directory, preventing path traversal via
// malicious repository names. Errors are logged but not propagated.
func removeCachedFiles(cacheDir, name string, log *slog.Logger) {
	root, err := os.OpenRoot(cacheDir)
	if err != nil {
		log.Warn(
			"failed to open cache directory",
			slog.String("cacheDir", cacheDir),
			slog.Any("error", err),
		)
		return
	}
	defer root.Close()

	for _, rel := range []string{
		helmpath.CacheIndexFile(name),
		helmpath.CacheChartsFile(name),
	} {
		if err := root.Remove(rel); err != nil && !errors.Is(err, fs.ErrNotExist) {
			log.Warn(
				"failed to remove cached file",
				slog.String("file", rel),
				slog.Any("error", err),
			)
		}
	}
}
