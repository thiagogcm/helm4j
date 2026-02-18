package search

import (
	"encoding/json"
	"errors"
	"fmt"
	"net/http"
	"net/url"
	"path"
	"time"
)

const (
	defaultHubEndpoint = "https://hub.helm.sh"
	hubSearchPath      = "api/chartsvc/v1/charts/search"
)

var errHubHostnameNotProvided = errors.New("no hostname provided")

func runHub(opts Options) ([]Result, error) {
	endpoint := opts.Endpoint
	if endpoint == "" {
		endpoint = defaultHubEndpoint
	}
	if err := validateHubEndpoint(endpoint); err != nil {
		return nil, fmt.Errorf("unable to create connection to %q: %w", endpoint, err)
	}

	searchURL, err := buildHubSearchURL(endpoint, opts.Keyword)
	if err != nil {
		return nil, err
	}

	req, err := http.NewRequest(http.MethodGet, searchURL, nil)
	if err != nil {
		return nil, err
	}
	req.Header.Set("User-Agent", "libhelm4j")

	hubClient := &http.Client{Timeout: 30 * time.Second}
	res, err := hubClient.Do(req)
	if err != nil {
		return nil, err
	}
	defer res.Body.Close()

	if res.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("failed to fetch %s : %s", searchURL, res.Status)
	}

	var payload hubSearchResponse
	if err := json.NewDecoder(res.Body).Decode(&payload); err != nil {
		return nil, fmt.Errorf("decode hub response: %w", err)
	}

	results := make([]Result, 0, len(payload.Data))
	for _, item := range payload.Data {
		chartURL := endpoint + "/charts/" + item.ID
		if item.ArtifactHub.PackageURL != "" {
			chartURL = item.ArtifactHub.PackageURL
		}

		name := item.Attributes.Name
		if name == "" {
			name = chartURL
		}

		results = append(results, Result{
			Name:           name,
			Version:        item.Relationships.LatestChartVersion.Data.Version,
			AppVersion:     item.Relationships.LatestChartVersion.Data.AppVersion,
			Description:    item.Attributes.Description,
			Score:          0,
			URL:            chartURL,
			RepositoryName: item.Attributes.Repo.Name,
			RepositoryURL:  item.Attributes.Repo.URL,
		})
	}

	if len(results) == 0 && opts.FailOnNoResult {
		return nil, errors.New("no results found")
	}

	return results, nil
}

func validateHubEndpoint(raw string) error {
	parsed, err := url.Parse(raw)
	if err != nil {
		return err
	}
	if parsed.Hostname() == "" {
		return errHubHostnameNotProvided
	}
	return nil
}

func buildHubSearchURL(endpoint, keyword string) (string, error) {
	parsed, err := url.Parse(endpoint)
	if err != nil {
		return "", err
	}
	parsed.Path = path.Join(parsed.Path, hubSearchPath)
	parsed.RawQuery = "q=" + url.QueryEscape(keyword)
	return parsed.String(), nil
}

type hubSearchResponse struct {
	Data []hubSearchResult `json:"data"`
}

type hubSearchResult struct {
	ID            string           `json:"id"`
	ArtifactHub   hubArtifactHub   `json:"artifactHub"`
	Attributes    hubChart         `json:"attributes"`
	Relationships hubRelationships `json:"relationships"`
}

type hubArtifactHub struct {
	PackageURL string `json:"packageUrl"`
}

type hubChart struct {
	Name        string  `json:"name"`
	Repo        hubRepo `json:"repo"`
	Description string  `json:"description"`
}

type hubRepo struct {
	Name string `json:"name"`
	URL  string `json:"url"`
}

type hubRelationships struct {
	LatestChartVersion hubLatestChartVersion `json:"latestChartVersion"`
}

type hubLatestChartVersion struct {
	Data hubChartVersion `json:"data"`
}

type hubChartVersion struct {
	Version    string    `json:"version"`
	AppVersion string    `json:"app_version"`
	Created    time.Time `json:"created"`
}
