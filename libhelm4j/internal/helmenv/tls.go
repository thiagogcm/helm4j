package helmenv

import (
	"crypto/tls"
	"crypto/x509"
	"errors"
	"fmt"
	"log/slog"
	"net/http"
	"os"

	"helm.sh/helm/v4/pkg/registry"

	"github.com/thiagogcm/libhelm4j/internal/helmlog"
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
			if perr, ok := errors.AsType[*os.PathError](err); ok {
				return nil, fmt.Errorf("load client certificate (%s %q): %w", perr.Op, perr.Path, err)
			}
			return nil, fmt.Errorf("load client certificate: %w", err)
		}
		tlsConf.Certificates = []tls.Certificate{cert}
	}

	if opts.CaFile != "" {
		caData, err := os.ReadFile(opts.CaFile)
		if err != nil {
			if perr, ok := errors.AsType[*os.PathError](err); ok {
				return nil, fmt.Errorf("read CA file (%s %q): %w", perr.Op, perr.Path, err)
			}
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
// If an unknown transport wrapper is encountered it is logged as a warning
// and the bare http.Transport from a new transport is returned as a fallback,
// so TLS injection still succeeds even when Helm introduces new wrapper types.
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
		// Unknown wrapper type: fall back to a plain http.Transport so TLS
		// injection can still succeed. The outer wrapper will carry the
		// injected TLS config through its own RoundTrip call.
		helmlog.Logger().Warn(
			"unknown registry transport wrapper; falling back to default http.Transport for TLS injection",
			slog.String("transportType", fmt.Sprintf("%T", roundTripper)),
		)
		return http.DefaultTransport.(*http.Transport).Clone(), nil
	}
}
