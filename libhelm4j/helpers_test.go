package main

import (
	"crypto/tls"
	"net/http"
	"testing"

	"helm.sh/helm/v4/pkg/registry"
)

func TestBuildTLSConfigSetsModernDefaults(t *testing.T) {
	tlsConfig, err := buildTLSConfig(registryOptions{InsecureSkipTLSVerify: true})
	if err != nil {
		t.Fatalf("build TLS config: %v", err)
	}

	if tlsConfig.MinVersion != tls.VersionTLS12 {
		t.Fatalf("expected min TLS version %d, got %d", tls.VersionTLS12, tlsConfig.MinVersion)
	}
	if !tlsConfig.InsecureSkipVerify {
		t.Fatal("expected insecure skip TLS verify to be propagated")
	}
}

func TestBuildTLSConfigRequiresCertificatePair(t *testing.T) {
	tests := []registryOptions{
		{CertFile: "/tmp/cert.pem"},
		{KeyFile: "/tmp/key.pem"},
	}

	for _, opts := range tests {
		_, err := buildTLSConfig(opts)
		if err == nil {
			t.Fatalf("expected cert/key mismatch to fail for options: %#v", opts)
		}
	}
}

func TestUnwrapHTTPTransportReturnsUnderlyingTransport(t *testing.T) {
	base := &http.Transport{}

	resolved, err := unwrapHTTPTransport(base)
	if err != nil {
		t.Fatalf("unwrap base transport: %v", err)
	}
	if resolved != base {
		t.Fatal("expected base transport to be returned")
	}

	wrapped := &registry.LoggingTransport{RoundTripper: base}
	resolved, err = unwrapHTTPTransport(wrapped)
	if err != nil {
		t.Fatalf("unwrap wrapped transport: %v", err)
	}
	if resolved != base {
		t.Fatal("expected wrapped transport to resolve to base transport")
	}
}

func TestUnwrapHTTPTransportRejectsUnsupportedTransport(t *testing.T) {
	_, err := unwrapHTTPTransport(roundTripperFunc(func(*http.Request) (*http.Response, error) {
		return nil, nil
	}))
	if err == nil {
		t.Fatal("expected unsupported transport to fail")
	}
}

type roundTripperFunc func(*http.Request) (*http.Response, error)

func (fn roundTripperFunc) RoundTrip(request *http.Request) (*http.Response, error) {
	return fn(request)
}
