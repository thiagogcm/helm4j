package helmenv

import (
	"io"
	"log/slog"
	"net/http"
	"os"

	"helm.sh/helm/v4/pkg/cli"
	"helm.sh/helm/v4/pkg/registry"

	"github.com/thiagogcm/libhelm4j/internal/helmlog"
)

// RegistryOptions captures the authentication and transport settings used
// to build an OCI registry client. These map 1:1 to the common Helm CLI
// flags (--cert-file, --key-file, --ca-file, etc.).
type RegistryOptions struct {
	CertFile              string
	KeyFile               string
	CaFile                string
	InsecureSkipTLSVerify bool
	PlainHTTP             bool
	Username              string
	Password              string
}

// BuildRegistryClient constructs a [registry.Client] honouring the common
// Helm flags captured in opts. The settings value provides the registry
// config path and debug flag.
func BuildRegistryClient(settings *cli.EnvSettings, opts RegistryOptions) (*registry.Client, error) {
	writer := io.Writer(io.Discard)
	if settings.Debug {
		writer = os.Stderr
	}

	helmlog.Logger().Debug(
		"building registry client",
		slog.Bool("plainHTTP", opts.PlainHTTP),
		slog.Bool("insecureSkipTLSVerify", opts.InsecureSkipTLSVerify),
		slog.Bool("hasClientCert", opts.CertFile != "" || opts.KeyFile != ""),
		slog.Bool("hasCAFile", opts.CaFile != ""),
	)

	clientOpts := []registry.ClientOption{
		registry.ClientOptDebug(settings.Debug),
		registry.ClientOptEnableCache(true),
		registry.ClientOptWriter(writer),
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

		retryTransport := registry.NewTransport(settings.Debug)
		httpTransport, err := unwrapHTTPTransport(retryTransport.Base)
		if err != nil {
			return nil, err
		}
		httpTransport.TLSClientConfig = tlsConfig

		clientOpts = append(clientOpts, registry.ClientOptHTTPClient(&http.Client{Transport: retryTransport}))
	}

	return registry.NewClient(clientOpts...)
}
