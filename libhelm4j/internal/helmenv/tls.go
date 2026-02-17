package helmenv

import (
	"crypto/tls"
	"crypto/x509"
	"errors"
	"fmt"
	"net/http"
	"os"

	"helm.sh/helm/v4/pkg/registry"
)

// buildTLSConfig creates a TLS configuration from the given registry options.
// It enforces TLS 1.2 as the minimum version.
func buildTLSConfig(opts RegistryOptions) (*tls.Config, error) {
	tlsConf := &tls.Config{ // #nosec G402
		InsecureSkipVerify: opts.InsecureSkipTLSVerify,
		MinVersion:         tls.VersionTLS12,
	}

	if opts.CertFile != "" || opts.KeyFile != "" {
		if opts.CertFile == "" || opts.KeyFile == "" {
			return nil, errors.New("both certFile and keyFile must be provided together")
		}

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

// unwrapHTTPTransport recursively strips Helm's wrapped transports to reach
// the underlying [http.Transport] so TLS settings can be injected.
func unwrapHTTPTransport(roundTripper http.RoundTripper) (*http.Transport, error) {
	if roundTripper == nil {
		return nil, errors.New("registry transport is nil")
	}

	switch transport := roundTripper.(type) {
	case *http.Transport:
		return transport, nil
	case *registry.LoggingTransport:
		return unwrapHTTPTransport(transport.RoundTripper)
	default:
		return nil, fmt.Errorf("unsupported registry transport %T", roundTripper)
	}
}
