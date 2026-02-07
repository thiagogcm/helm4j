# Helm4j

Helm4j is a Java library that provides bindings to the Helm package manager for Kubernetes using Go's c-shared build mode and Java's FFM APIs with Jextract codegen tool.

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
