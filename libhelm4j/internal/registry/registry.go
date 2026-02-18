// Package registry implements `helm registry login` and `helm registry logout`.
package registry

import (
	"errors"
	"fmt"
	"io"
	"log/slog"
	"strings"

	"helm.sh/helm/v4/pkg/action"

	"github.com/thiagogcm/libhelm4j/internal/bridge"
	"github.com/thiagogcm/libhelm4j/internal/helmenv"
	"github.com/thiagogcm/libhelm4j/internal/helmlog"
)

// Options captures the authentication and TLS settings for registry operations.
type Options struct {
	Username  string `json:"username,omitempty"`
	Password  string `json:"password,omitempty"`
	CertFile  string `json:"certFile,omitempty"`
	KeyFile   string `json:"keyFile,omitempty"`
	CaFile    string `json:"caFile,omitempty"`
	Insecure  bool   `json:"insecure,omitempty"`
	PlainHTTP bool   `json:"plainHttp,omitempty"`
}

// Response is the top-level JSON payload returned across the FFM boundary.
type Response struct {
	Hostname string `json:"hostname"`
	Status   string `json:"status"`
}

// Run executes a helm registry login or logout operation.
func Run(mode, hostname string, opts Options) (string, error) {
	log := helmlog.Logger().With(
		slog.String("operation", "registry"),
		slog.String("mode", mode),
		slog.String("hostname", hostname),
	)

	if strings.TrimSpace(hostname) == "" {
		return "", errors.New("hostname is required")
	}

	switch strings.ToLower(strings.TrimSpace(mode)) {
	case "login":
		return runLogin(log, hostname, opts)
	case "logout":
		return runLogout(log, hostname)
	case "":
		return "", errors.New("registry mode is required (login or logout)")
	default:
		return "", fmt.Errorf("unsupported registry mode: %s", mode)
	}
}

func runLogin(log *slog.Logger, hostname string, opts Options) (string, error) {
	log.Debug("running helm registry login")

	env, err := helmenv.New()
	if err != nil {
		return "", fmt.Errorf("bootstrap helm: %w", err)
	}

	loginOpts := []action.RegistryLoginOpt{
		action.WithInsecure(opts.Insecure),
		action.WithPlainHTTPLogin(opts.PlainHTTP),
	}
	if opts.CertFile != "" {
		loginOpts = append(loginOpts, action.WithCertFile(opts.CertFile))
	}
	if opts.KeyFile != "" {
		loginOpts = append(loginOpts, action.WithKeyFile(opts.KeyFile))
	}
	if opts.CaFile != "" {
		loginOpts = append(loginOpts, action.WithCAFile(opts.CaFile))
	}

	client := action.NewRegistryLogin(env.Config)
	if err := client.Run(io.Discard, hostname, opts.Username, opts.Password, loginOpts...); err != nil {
		return "", fmt.Errorf("helm registry login: %w", err)
	}

	resp := Response{Hostname: hostname, Status: "ok"}
	result, err := bridge.MarshalJSON(resp)
	if err != nil {
		return "", err
	}

	log.Debug("helm registry login completed successfully")
	return result, nil
}

func runLogout(log *slog.Logger, hostname string) (string, error) {
	log.Debug("running helm registry logout")

	env, err := helmenv.New()
	if err != nil {
		return "", fmt.Errorf("bootstrap helm: %w", err)
	}

	client := action.NewRegistryLogout(env.Config)
	if err := client.Run(io.Discard, hostname); err != nil {
		return "", fmt.Errorf("helm registry logout: %w", err)
	}

	resp := Response{Hostname: hostname, Status: "ok"}
	result, err := bridge.MarshalJSON(resp)
	if err != nil {
		return "", err
	}

	log.Debug("helm registry logout completed successfully")
	return result, nil
}
