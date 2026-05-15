# hello-world

A minimal ConfigMap-only Helm chart bundled with the helm4j samples module to exercise the SDK
end to end against a real cluster. The release renders a single ConfigMap and a Helm `test`
hook that runs a short-lived Pod to validate `release.test()`.
