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

// ApplyChartPathOptions copies the fields from [ChartPathOpts] into an
// [action.ChartPathOptions] value. Devel is intentionally excluded because
// it is a field on the action struct itself, not on ChartPathOptions.
func ApplyChartPathOptions(cpo *action.ChartPathOptions, src ChartPathOpts) {
	cpo.Version = src.Version
	cpo.RepoURL = src.RepoURL
	cpo.Username = src.Username
	cpo.Password = src.Password
	cpo.PlainHTTP = src.PlainHTTP
	cpo.InsecureSkipTLSVerify = src.InsecureSkipTLSVerify
	cpo.Keyring = src.Keyring
	cpo.CertFile = src.CertFile
	cpo.KeyFile = src.KeyFile
	cpo.CaFile = src.CaFile
	cpo.PassCredentialsAll = src.PassCredentialsAll
	cpo.Verify = src.Verify
}

// RegistryOptsFromChartPath derives [RegistryOptions] from the authentication
// and transport fields present in [ChartPathOpts], avoiding duplicate field
// copying when building a registry client.
func RegistryOptsFromChartPath(src ChartPathOpts) RegistryOptions {
	return RegistryOptions{
		CertFile:              src.CertFile,
		KeyFile:               src.KeyFile,
		CaFile:                src.CaFile,
		InsecureSkipTLSVerify: src.InsecureSkipTLSVerify,
		PlainHTTP:             src.PlainHTTP,
		Username:              src.Username,
		Password:              src.Password,
	}
}
