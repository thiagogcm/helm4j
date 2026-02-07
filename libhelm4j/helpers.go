package main

/*
#include <stdlib.h>
*/
import "C"

import (
	"crypto/tls"
	"crypto/x509"
	"encoding/json"
	"fmt"
	"net/http"
	"os"

	"helm.sh/helm/v4/pkg/action"
	chart "helm.sh/helm/v4/pkg/chart/v2"
	"helm.sh/helm/v4/pkg/chart/v2/loader"
	"helm.sh/helm/v4/pkg/cli"
	"helm.sh/helm/v4/pkg/registry"
)

type helmEnv struct {
	Settings *cli.EnvSettings
	Config   *action.Configuration
}

type registryOptions struct {
	CertFile              string
	KeyFile               string
	CaFile                string
	InsecureSkipTLSVerify bool
	PlainHTTP             bool
	Username              string
	Password              string
}

// newHelmEnv provisions a fresh Helm CLI environment and configuration.
// It mirrors the initialization used by the helm CLI so future actions can reuse it.
func newHelmEnv() (*helmEnv, error) {
	settings := cli.New()
	cfg := action.NewConfiguration()
	if err := cfg.Init(settings.RESTClientGetter(), settings.Namespace(), os.Getenv("HELM_DRIVER")); err != nil {
		return nil, fmt.Errorf("init action configuration: %w", err)
	}
	return &helmEnv{Settings: settings, Config: cfg}, nil
}

// buildRegistryClient constructs a registry client honoring common Helm flags.
func buildRegistryClient(settings *cli.EnvSettings, opts registryOptions) (*registry.Client, error) {
	clientOpts := []registry.ClientOption{
		registry.ClientOptDebug(settings.Debug),
		registry.ClientOptEnableCache(true),
		registry.ClientOptWriter(os.Stderr),
		registry.ClientOptCredentialsFile(settings.RegistryConfig),
		registry.ClientOptBasicAuth(opts.Username, opts.Password),
	}

	if opts.PlainHTTP {
		clientOpts = append(clientOpts, registry.ClientOptPlainHTTP())
	}

	if opts.CertFile != "" || opts.KeyFile != "" || opts.CaFile != "" || opts.InsecureSkipTLSVerify {
		tlsConfig, err := buildTLSConfig(opts)
		if err != nil {
			return nil, err
		}
		clientOpts = append(clientOpts, registry.ClientOptHTTPClient(&http.Client{Transport: &http.Transport{TLSClientConfig: tlsConfig}}))
	}

	return registry.NewClient(clientOpts...)
}

func buildTLSConfig(opts registryOptions) (*tls.Config, error) {
	tlsConf := &tls.Config{InsecureSkipVerify: opts.InsecureSkipTLSVerify} // #nosec G402

	if opts.CertFile != "" || opts.KeyFile != "" {
		cert, err := tls.LoadX509KeyPair(opts.CertFile, opts.KeyFile)
		if err != nil {
			return nil, fmt.Errorf("load client certificate: %w", err)
		}
		tlsConf.Certificates = []tls.Certificate{cert}
	}

	if opts.CaFile != "" {
		caData, err := os.ReadFile(opts.CaFile)
		if err != nil {
			return nil, fmt.Errorf("read CA file: %w", err)
		}
		pool := x509.NewCertPool()
		if ok := pool.AppendCertsFromPEM(caData); !ok {
			return nil, fmt.Errorf("parse CA file: %s", opts.CaFile)
		}
		tlsConf.RootCAs = pool
	}

	return tlsConf, nil
}

// locateAndLoadChart resolves the chart reference to disk and loads it.
func locateAndLoadChart(client *action.Show, settings *cli.EnvSettings, chartRef string) (string, *chart.Chart, error) {
	if client.Version == "" && client.Devel {
		client.Version = ">0.0.0-0"
	}

	chartPath, err := client.LocateChart(chartRef, settings)
	if err != nil {
		return "", nil, fmt.Errorf("locate chart: %w", err)
	}

	ch, err := loader.Load(chartPath)
	if err != nil {
		return "", nil, fmt.Errorf("load chart: %w", err)
	}

	return chartPath, ch, nil
}

func marshalJSON(value any) (string, error) {
	b, err := json.Marshal(value)
	if err != nil {
		return "", err
	}
	return string(b), nil
}

func goString(cstr *C.char) string {
	if cstr == nil {
		return ""
	}
	return C.GoString(cstr)
}

func toCString(value string) *C.char {
	return C.CString(value)
}
