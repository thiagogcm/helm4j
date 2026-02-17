// Package template will implement `helm template` — client-side chart
// rendering without contacting a Kubernetes cluster. This is typically the
// most requested Helm SDK operation after show and search.
//
// TODO: implement Run(chartRef string, opts Options) (string, error)
package template
