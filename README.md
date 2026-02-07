# Helm4j

Helm4j is a Java library that provides bindings to Helm for Kubernetes, backed by Go `c-shared` binaries and Java FFM APIs.

## Public API (Java 25)

`HelmClient` exposes a Java-first surface and hides native/jextract payload details.

```java
import dev.nthings.helm4j.client.HelmClient;
import dev.nthings.helm4j.client.HelmClientFactory;
import dev.nthings.helm4j.options.SearchOptions;
import dev.nthings.helm4j.options.ShowOptions;

var client = HelmClientFactory.create().newClient();

var chart = client.showChart("bitnami/nginx");
System.out.println(chart.metadataYaml());

var values =
    client.showValues(
        "bitnami/nginx",
        ShowOptions.builder().version("19.0.0").includePreReleaseVersions(false).build());
System.out.println(values.valuesYaml());

var search =
    client.search(
        SearchOptions.builder()
            .query("nginx")
            .includeAllVersions(false)
            .versionConstraint(">=1.0.0")
            .build());

search.first().ifPresent(result -> System.out.println(result.name()));
```

## Build libhelm4j and generate bindings

```bash
export LLVM_HOME=/usr/lib/llvm-18
export LD_LIBRARY_PATH=$LLVM_HOME/lib

go build -buildmode=c-shared -o libhelm4j.so .

jextract -Djava.library.path=$LLVM_HOME -Ilibhelm4j -l:libhelm4j/libhelm4j.so \
  --include-function FreeString \
  --include-function HelmShowChart \
  --include-function HelmShowValues \
  --include-function HelmShowReadme \
  --include-function HelmShowAll \
  --include-function HelmSearch \
  --output src/main/generated --target-package dev.nthings.helm4j.jextract libhelm4j/libhelm4j.h
```
