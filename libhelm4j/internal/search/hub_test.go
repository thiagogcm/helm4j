package search

import (
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
)

func TestRunHubMapsResults(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path != "/api/chartsvc/v1/charts/search" {
			t.Fatalf("unexpected path: %s", r.URL.Path)
		}
		if r.URL.Query().Get("q") != "nginx ingress" {
			t.Fatalf("unexpected query: %s", r.URL.RawQuery)
		}
		_, _ = w.Write([]byte(`
      {"data":[{"id":"bitnami/nginx","artifactHub":{"packageUrl":"https://artifacthub.io/packages/helm/bitnami/nginx"},"attributes":{"name":"nginx","description":"Nginx chart","repo":{"name":"bitnami","url":"https://charts.bitnami.com/bitnami"}},"relationships":{"latestChartVersion":{"data":{"version":"19.0.0","app_version":"1.27.0"}}}}]}
    `))
	}))
	defer server.Close()

	results, err := Run(ModeHub, Options{Keyword: "nginx ingress", Endpoint: server.URL})
	if err != nil {
		t.Fatalf("expected hub search to succeed, got: %v", err)
	}
	if len(results) != 1 {
		t.Fatalf("expected one result, got: %d", len(results))
	}

	got := results[0]
	if got.Name != "nginx" {
		t.Fatalf("expected name nginx, got: %s", got.Name)
	}
	if got.URL != "https://artifacthub.io/packages/helm/bitnami/nginx" {
		t.Fatalf("unexpected package url: %s", got.URL)
	}
	if got.RepositoryName != "bitnami" {
		t.Fatalf("unexpected repository name: %s", got.RepositoryName)
	}
	if got.RepositoryURL != "https://charts.bitnami.com/bitnami" {
		t.Fatalf("unexpected repository url: %s", got.RepositoryURL)
	}
}

func TestRunHubFailOnNoResult(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		_, _ = w.Write([]byte(`{"data":[]}`))
	}))
	defer server.Close()

	_, err := Run(ModeHub, Options{Keyword: "missing", Endpoint: server.URL, FailOnNoResult: true})
	if err == nil {
		t.Fatal("expected fail-on-no-result to return an error")
	}
	if !strings.Contains(err.Error(), "no results found") {
		t.Fatalf("expected no results found error, got: %v", err)
	}
}

func TestRunHubInvalidEndpoint(t *testing.T) {
	_, err := Run(ModeHub, Options{Keyword: "nginx", Endpoint: "http:///missing-host"})
	if err == nil {
		t.Fatal("expected invalid endpoint to return an error")
	}
	if !strings.Contains(err.Error(), "no hostname provided") {
		t.Fatalf("expected hostname validation error, got: %v", err)
	}
}
