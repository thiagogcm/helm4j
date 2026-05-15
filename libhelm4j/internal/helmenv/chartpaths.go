package helmenv

import "helm.sh/helm/v4/pkg/action"

// ChartPathOpts carries the chart-resolution flags that are common across
// install, upgrade, template, show, and pull operations. Embedding this
// struct in each operation's Options type promotes the fields for JSON
// marshalling and eliminates the repeated 12-line ChartPathOptions block.
type ChartPathOpts struct {
	Version               string `json:"version,omitempty"`
	RepoURL               string `json:"repo,omitempty"`
	Username              string `json:"username,omitempty"`
	Password              string `json:"password,omitempty"`
	PlainHTTP             bool   `json:"plainHttp,omitempty"`
	InsecureSkipTLSVerify bool   `json:"insecureSkipTlsVerify,omitempty"`
	Keyring               string `json:"keyring,omitempty"`
	CertFile              string `json:"certFile,omitempty"`
	KeyFile               string `json:"keyFile,omitempty"`
	CaFile                string `json:"caFile,omitempty"`
	PassCredentialsAll    bool   `json:"passCredentialsAll,omitempty"`
	Verify                bool   `json:"verify,omitempty"`
	Devel                 bool   `json:"devel,omitempty"`
}

// ApplyTo copies the fields from this [ChartPathOpts] into an
// [action.ChartPathOptions] value. Devel is intentionally excluded because
// it is a field on the action struct itself, not on ChartPathOptions.
func (c ChartPathOpts) ApplyTo(cpo *action.ChartPathOptions) {
	cpo.Version = c.Version
	cpo.RepoURL = c.RepoURL
	cpo.Username = c.Username
	cpo.Password = c.Password
	cpo.PlainHTTP = c.PlainHTTP
	cpo.InsecureSkipTLSVerify = c.InsecureSkipTLSVerify
	cpo.Keyring = c.Keyring
	cpo.CertFile = c.CertFile
	cpo.KeyFile = c.KeyFile
	cpo.CaFile = c.CaFile
	cpo.PassCredentialsAll = c.PassCredentialsAll
	cpo.Verify = c.Verify
}

// RegistryOptions derives [RegistryOptions] from the authentication and
// transport fields present in this [ChartPathOpts], avoiding duplicate field
// copying when building a registry client.
func (c ChartPathOpts) RegistryOptions() RegistryOptions {
	return RegistryOptions{
		CertFile:              c.CertFile,
		KeyFile:               c.KeyFile,
		CaFile:                c.CaFile,
		InsecureSkipTLSVerify: c.InsecureSkipTLSVerify,
		PlainHTTP:             c.PlainHTTP,
		Username:              c.Username,
		Password:              c.Password,
	}
}
