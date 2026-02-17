package repomgr

import (
	"errors"
	"os"
	"path/filepath"
	"strings"
	"testing"
	"time"
)

// ---------------------------------------------------------------------------
// Test environment helper (mirrors search_test.go)
// ---------------------------------------------------------------------------

func configureTestEnv(t *testing.T, testHome, repoConfig, repoCache string) {
	t.Helper()

	t.Setenv("HELM_CONFIG_HOME", filepath.Join(testHome, "config"))
	t.Setenv("HELM_CACHE_HOME", filepath.Join(testHome, "cache"))
	t.Setenv("HELM_DATA_HOME", filepath.Join(testHome, "data"))
	t.Setenv("HELM_REPOSITORY_CONFIG", repoConfig)
	t.Setenv("HELM_REPOSITORY_CACHE", repoCache)
	t.Setenv("HELM_REGISTRY_CONFIG", filepath.Join(testHome, "registry", "config.json"))
	t.Setenv("HELM_DEBUG", "false")

	if err := os.MkdirAll(repoCache, 0o755); err != nil {
		t.Fatalf("create repository cache dir: %v", err)
	}
}

// writeRepoConfig writes a repositories.yaml file with the given YAML body.
func writeRepoConfig(t *testing.T, path, content string) {
	t.Helper()
	if err := os.MkdirAll(filepath.Dir(path), 0o755); err != nil {
		t.Fatalf("mkdir for repo config: %v", err)
	}
	if err := os.WriteFile(path, []byte(content), 0o600); err != nil {
		t.Fatalf("write repo config: %v", err)
	}
}

// sampleRepoConfig returns a repositories.yaml with two entries.
func sampleRepoConfig() string {
	return `apiVersion: v1
repositories:
  - name: stable
    url: https://charts.helm.sh/stable
  - name: bitnami
    url: https://charts.bitnami.com/bitnami
`
}

// ---------------------------------------------------------------------------
// Add tests
// ---------------------------------------------------------------------------

func TestAddMissingNameReturnsError(t *testing.T) {
	testHome := t.TempDir()
	repoConfig := filepath.Join(testHome, "repositories.yaml")
	repoCache := filepath.Join(testHome, "repository-cache")
	configureTestEnv(t, testHome, repoConfig, repoCache)

	_, err := Add(AddOptions{URL: "https://example.com"})
	if err == nil {
		t.Fatal("expected error for missing name")
	}
	if !strings.Contains(err.Error(), "name is required") {
		t.Fatalf("unexpected error: %v", err)
	}
}

func TestAddMissingURLReturnsError(t *testing.T) {
	testHome := t.TempDir()
	repoConfig := filepath.Join(testHome, "repositories.yaml")
	repoCache := filepath.Join(testHome, "repository-cache")
	configureTestEnv(t, testHome, repoConfig, repoCache)

	_, err := Add(AddOptions{Name: "myrepo"})
	if err == nil {
		t.Fatal("expected error for missing url")
	}
	if !strings.Contains(err.Error(), "URL is required") {
		t.Fatalf("unexpected error: %v", err)
	}
}

func TestAddDuplicateNameWithoutForceUpdateReturnsError(t *testing.T) {
	testHome := t.TempDir()
	repoConfig := filepath.Join(testHome, "repositories.yaml")
	repoCache := filepath.Join(testHome, "repository-cache")
	configureTestEnv(t, testHome, repoConfig, repoCache)

	writeRepoConfig(t, repoConfig, sampleRepoConfig())

	_, err := Add(AddOptions{Name: "stable", URL: "https://charts.helm.sh/stable"})
	if err == nil {
		t.Fatal("expected error for duplicate name")
	}
	if !errors.Is(err, ErrRepositoryAlreadyExists) {
		t.Fatalf("expected ErrRepositoryAlreadyExists, got: %v", err)
	}
}

func TestAddDeprecatedRepositoryBlockedByDefault(t *testing.T) {
	_, err := Add(AddOptions{Name: "stable", URL: "https://kubernetes-charts.storage.googleapis.com"})
	if err == nil {
		t.Fatal("expected error for deprecated repository URL")
	}
	if !strings.Contains(err.Error(), "no longer available") {
		t.Fatalf("unexpected error: %v", err)
	}
}

// ---------------------------------------------------------------------------
// List tests
// ---------------------------------------------------------------------------

func TestListMissingConfigReturnsError(t *testing.T) {
	testHome := t.TempDir()
	repoConfig := filepath.Join(testHome, "repositories.yaml")
	repoCache := filepath.Join(testHome, "repository-cache")
	configureTestEnv(t, testHome, repoConfig, repoCache)

	_, err := List(ListOptions{})
	if err == nil {
		t.Fatal("expected error for missing config")
	}
	if !errors.Is(err, ErrNoRepositoriesConfigured) {
		t.Fatalf("expected ErrNoRepositoriesConfigured, got: %v", err)
	}
}

func TestListEmptyConfigReturnsError(t *testing.T) {
	testHome := t.TempDir()
	repoConfig := filepath.Join(testHome, "repositories.yaml")
	repoCache := filepath.Join(testHome, "repository-cache")
	configureTestEnv(t, testHome, repoConfig, repoCache)

	writeRepoConfig(t, repoConfig, "repositories: []\n")

	_, err := List(ListOptions{})
	if err == nil {
		t.Fatal("expected error for empty config")
	}
	if !errors.Is(err, ErrNoRepositoriesConfigured) {
		t.Fatalf("expected ErrNoRepositoriesConfigured, got: %v", err)
	}
}

func TestListReturnsConfiguredRepositories(t *testing.T) {
	testHome := t.TempDir()
	repoConfig := filepath.Join(testHome, "repositories.yaml")
	repoCache := filepath.Join(testHome, "repository-cache")
	configureTestEnv(t, testHome, repoConfig, repoCache)

	writeRepoConfig(t, repoConfig, sampleRepoConfig())

	resp, err := List(ListOptions{})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if len(resp.Repositories) != 2 {
		t.Fatalf("expected 2 repositories, got %d", len(resp.Repositories))
	}
	if resp.Repositories[0].Name != "stable" {
		t.Fatalf("expected first repo name 'stable', got %q", resp.Repositories[0].Name)
	}
	if resp.Repositories[1].Name != "bitnami" {
		t.Fatalf("expected second repo name 'bitnami', got %q", resp.Repositories[1].Name)
	}
}

// ---------------------------------------------------------------------------
// Remove tests
// ---------------------------------------------------------------------------

func TestRemoveEmptyNamesReturnsError(t *testing.T) {
	testHome := t.TempDir()
	repoConfig := filepath.Join(testHome, "repositories.yaml")
	repoCache := filepath.Join(testHome, "repository-cache")
	configureTestEnv(t, testHome, repoConfig, repoCache)

	_, err := Remove(RemoveOptions{Names: []string{}})
	if err == nil {
		t.Fatal("expected error for empty names")
	}
	if !strings.Contains(err.Error(), "at least one repository name is required") {
		t.Fatalf("unexpected error: %v", err)
	}
}

func TestRemoveNonExistentRepoReturnsError(t *testing.T) {
	testHome := t.TempDir()
	repoConfig := filepath.Join(testHome, "repositories.yaml")
	repoCache := filepath.Join(testHome, "repository-cache")
	configureTestEnv(t, testHome, repoConfig, repoCache)

	writeRepoConfig(t, repoConfig, sampleRepoConfig())

	_, err := Remove(RemoveOptions{Names: []string{"nonexistent"}})
	if err == nil {
		t.Fatal("expected error for non-existent repo")
	}
	if !errors.Is(err, ErrRepositoryNotFound) {
		t.Fatalf("expected ErrRepositoryNotFound, got: %v", err)
	}
}

func TestRemoveExistingRepoSucceeds(t *testing.T) {
	testHome := t.TempDir()
	repoConfig := filepath.Join(testHome, "repositories.yaml")
	repoCache := filepath.Join(testHome, "repository-cache")
	configureTestEnv(t, testHome, repoConfig, repoCache)

	writeRepoConfig(t, repoConfig, sampleRepoConfig())

	// Create fake cached files to verify cleanup.
	indexFile := filepath.Join(repoCache, "stable-index.yaml")
	chartsFile := filepath.Join(repoCache, "stable-charts.txt")
	os.WriteFile(indexFile, []byte("fake"), 0o600)
	os.WriteFile(chartsFile, []byte("fake"), 0o600)

	resp, err := Remove(RemoveOptions{Names: []string{"stable"}})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if len(resp.Removed) != 1 || resp.Removed[0] != "stable" {
		t.Fatalf("expected [stable] removed, got %v", resp.Removed)
	}

	// Verify the config was rewritten without "stable".
	listResp, err := List(ListOptions{})
	if err != nil {
		t.Fatalf("list after remove failed: %v", err)
	}
	if len(listResp.Repositories) != 1 {
		t.Fatalf("expected 1 repo remaining, got %d", len(listResp.Repositories))
	}
	if listResp.Repositories[0].Name != "bitnami" {
		t.Fatalf("expected remaining repo 'bitnami', got %q", listResp.Repositories[0].Name)
	}

	// Verify cached files are deleted.
	if _, err := os.Stat(indexFile); !os.IsNotExist(err) {
		t.Fatal("expected cached index file to be removed")
	}
	if _, err := os.Stat(chartsFile); !os.IsNotExist(err) {
		t.Fatal("expected cached charts file to be removed")
	}
}

func TestRemoveMultipleReposSucceeds(t *testing.T) {
	testHome := t.TempDir()
	repoConfig := filepath.Join(testHome, "repositories.yaml")
	repoCache := filepath.Join(testHome, "repository-cache")
	configureTestEnv(t, testHome, repoConfig, repoCache)

	writeRepoConfig(t, repoConfig, sampleRepoConfig())

	resp, err := Remove(RemoveOptions{Names: []string{"stable", "bitnami"}})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if len(resp.Removed) != 2 {
		t.Fatalf("expected 2 removed, got %d", len(resp.Removed))
	}
}

func TestRemoveMissingConfigReturnsError(t *testing.T) {
	testHome := t.TempDir()
	repoConfig := filepath.Join(testHome, "repositories.yaml")
	repoCache := filepath.Join(testHome, "repository-cache")
	configureTestEnv(t, testHome, repoConfig, repoCache)

	_, err := Remove(RemoveOptions{Names: []string{"stable"}})
	if err == nil {
		t.Fatal("expected error for missing config")
	}
	if !errors.Is(err, ErrNoRepositoriesConfigured) {
		t.Fatalf("expected ErrNoRepositoriesConfigured, got: %v", err)
	}
}

// ---------------------------------------------------------------------------
// Update tests
// ---------------------------------------------------------------------------

func TestUpdateMissingConfigReturnsError(t *testing.T) {
	testHome := t.TempDir()
	repoConfig := filepath.Join(testHome, "repositories.yaml")
	repoCache := filepath.Join(testHome, "repository-cache")
	configureTestEnv(t, testHome, repoConfig, repoCache)

	_, err := Update(UpdateOptions{})
	if err == nil {
		t.Fatal("expected error for missing config")
	}
	if !errors.Is(err, ErrNoRepositoriesConfigured) {
		t.Fatalf("expected ErrNoRepositoriesConfigured, got: %v", err)
	}
}

func TestUpdateUnknownNameReturnsError(t *testing.T) {
	testHome := t.TempDir()
	repoConfig := filepath.Join(testHome, "repositories.yaml")
	repoCache := filepath.Join(testHome, "repository-cache")
	configureTestEnv(t, testHome, repoConfig, repoCache)

	writeRepoConfig(t, repoConfig, sampleRepoConfig())

	_, err := Update(UpdateOptions{Names: []string{"nonexistent"}})
	if err == nil {
		t.Fatal("expected error for unknown repo name")
	}
	if !errors.Is(err, ErrRepositoryNotFound) {
		t.Fatalf("expected ErrRepositoryNotFound, got: %v", err)
	}
}

func TestParseTimeoutFallsBackToDefaultWhenInvalid(t *testing.T) {
	got := parseTimeout("not-a-duration")
	want := time.Duration(120) * time.Second
	if got != want {
		t.Fatalf("expected default timeout %v, got %v", want, got)
	}
}

func TestParseTimeoutParsesValidDurations(t *testing.T) {
	got := parseTimeout("5s")
	if got != 5*time.Second {
		t.Fatalf("expected 5s, got %v", got)
	}
}

// ---------------------------------------------------------------------------
// loadRepoFile / loadOrCreateRepoFile helper tests
// ---------------------------------------------------------------------------

func TestLoadRepoFileInvalidYAMLReturnsError(t *testing.T) {
	testHome := t.TempDir()
	repoConfig := filepath.Join(testHome, "repositories.yaml")
	repoCache := filepath.Join(testHome, "repository-cache")
	configureTestEnv(t, testHome, repoConfig, repoCache)

	writeRepoConfig(t, repoConfig, "repositories: [")

	_, err := List(ListOptions{})
	if err == nil {
		t.Fatal("expected error for invalid YAML")
	}
	if !strings.Contains(err.Error(), "load repository config") {
		t.Fatalf("expected wrapped config error, got: %v", err)
	}
}

func TestSelectEntriesUnknownNameReturnsError(t *testing.T) {
	testHome := t.TempDir()
	repoConfig := filepath.Join(testHome, "repositories.yaml")
	repoCache := filepath.Join(testHome, "repository-cache")
	configureTestEnv(t, testHome, repoConfig, repoCache)

	writeRepoConfig(t, repoConfig, sampleRepoConfig())

	_, err := Update(UpdateOptions{Names: []string{"stable", "nonexistent"}})
	if err == nil {
		t.Fatal("expected error for unknown repo name in selectEntries")
	}
	if !errors.Is(err, ErrRepositoryNotFound) {
		t.Fatalf("expected ErrRepositoryNotFound, got: %v", err)
	}
}
