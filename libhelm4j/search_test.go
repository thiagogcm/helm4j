package main

import (
	"errors"
	"log/slog"
	"os"
	"path/filepath"
	"strings"
	"testing"
)

func TestSetNativeLogLevelFromEnv(t *testing.T) {
	t.Setenv("HELM_DEBUG", "true")
	if enabled := setNativeLogLevelFromEnv(); !enabled {
		t.Fatal("expected HELM_DEBUG=true to enable debug logging")
	}
	if level := nativeLogLevel.Level(); level != slog.LevelDebug {
		t.Fatalf("expected slog level debug, got %v", level)
	}

	t.Setenv("HELM_DEBUG", "false")
	if enabled := setNativeLogLevelFromEnv(); enabled {
		t.Fatal("expected HELM_DEBUG=false to disable debug logging")
	}
	if level := nativeLogLevel.Level(); level != slog.LevelWarn {
		t.Fatalf("expected slog level warn, got %v", level)
	}
}

func TestRunSearchMissingRepositoryConfigReturnsError(t *testing.T) {
	testHome := t.TempDir()
	repoConfig := filepath.Join(testHome, "repositories.yaml")
	repoCache := filepath.Join(testHome, "repository-cache")

	configureSearchTestEnv(t, testHome, repoConfig, repoCache)

	_, err := runSearch(SearchOptions{Keyword: "demo"})
	if err == nil {
		t.Fatal("expected missing repository config to return an error")
	}
	if !errors.Is(err, errNoRepositoriesConfigured) {
		t.Fatalf("expected no repositories configured error, got: %v", err)
	}
}

func TestRunSearchEmptyRepositoryConfigReturnsError(t *testing.T) {
	testHome := t.TempDir()
	repoConfig := filepath.Join(testHome, "repositories.yaml")
	repoCache := filepath.Join(testHome, "repository-cache")

	if err := os.WriteFile(repoConfig, []byte("repositories: []\n"), 0o600); err != nil {
		t.Fatalf("write empty repository config: %v", err)
	}

	configureSearchTestEnv(t, testHome, repoConfig, repoCache)

	_, err := runSearch(SearchOptions{Keyword: "demo"})
	if err == nil {
		t.Fatal("expected empty repository config to return an error")
	}
	if !errors.Is(err, errNoRepositoriesConfigured) {
		t.Fatalf("expected no repositories configured error, got: %v", err)
	}
}

func TestRunSearchInvalidRepositoryConfigReturnsError(t *testing.T) {
	testHome := t.TempDir()
	repoConfig := filepath.Join(testHome, "repositories.yaml")
	repoCache := filepath.Join(testHome, "repository-cache")

	if err := os.WriteFile(repoConfig, []byte("repositories: ["), 0o600); err != nil {
		t.Fatalf("write invalid repository config: %v", err)
	}

	configureSearchTestEnv(t, testHome, repoConfig, repoCache)

	_, err := runSearch(SearchOptions{Keyword: "demo"})
	if err == nil {
		t.Fatal("expected invalid repository config to return an error")
	}
	if !strings.Contains(err.Error(), "load repository config") {
		t.Fatalf("expected wrapped repository config error, got: %v", err)
	}
}

func configureSearchTestEnv(t *testing.T, testHome string, repoConfig string, repoCache string) {
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
