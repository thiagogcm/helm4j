# List available commands
help:
  @just --list

# Build the full project (Go + Java)
build: go-build
  ./gradlew build --no-daemon

# Run Java tests
test:
  ./gradlew test --no-daemon

# Run build + tests + coverage
check: go-all check-native-parity build coverage
  @echo "✓ All checks passed"

# Clean all build artifacts
clean:
  ./gradlew clean --no-daemon
  rm -f libhelm4j/libhelm4j.so libhelm4j/libhelm4j.h *.hprof

# Generate Java test coverage report
coverage:
  ./gradlew test jacocoTestReport jacocoTestCoverageVerification --no-daemon
  @echo "Coverage reports: helm4j-*/build/reports/jacoco/test/html/index.html"

# Build the Go shared library
go-build:
  cd libhelm4j && CGO_ENABLED=1 go build -buildmode=c-shared -trimpath -ldflags="-s -w -extldflags=-Wl,--version-script=$PWD/libhelm4j.map" -o libhelm4j.so .

# Run Go unit tests
go-test:
  cd libhelm4j && go test -v -count=1 ./internal/...

# Run go vet on the Go module
go-vet:
  cd libhelm4j && go vet ./...

# Run go fix modernisers (Go 1.26+)
go-fix:
  cd libhelm4j && go fix ./...

# Run go-vet + go-test + go-build
go-all: go-vet go-test go-build
  @echo "✓ Go pipeline passed"

# Regenerate Java FFM bindings from libhelm4j.h
jextract: go-build
  #!/usr/bin/env bash
  set -euo pipefail
  if [[ -z "${LLVM_HOME:-}" ]]; then
    echo "LLVM_HOME is required (example: export LLVM_HOME=/usr/lib/llvm-18)"
    exit 1
  fi
  LD_LIBRARY_PATH="${LLVM_HOME}/lib" jextract -Djava.library.path="${LLVM_HOME}" -Ilibhelm4j -l:libhelm4j/libhelm4j.so \
    --include-function FreeString \
    --include-function HelmShow \
    --include-function HelmInstall \
    --include-function HelmSearch \
    --include-function HelmRepo \
    --include-function HelmUpgrade \
    --include-function HelmUninstall \
    --include-function HelmStatus \
    --include-function HelmRollback \
    --include-function HelmHistory \
    --include-function HelmGet \
    --include-function HelmList \
    --include-function HelmPull \
    --include-function HelmPush \
    --include-function HelmPackage \
    --include-function HelmDependency \
    --include-function HelmRegistry \
    --include-function HelmTest \
    --include-function HelmTemplate \
    --include-function HelmLint \
    --include-function HelmVersion \
    --output helm4j-runtime-native/src/main/generated --target-package dev.nthings.helm4j.jextract libhelm4j/libhelm4j.h
  # jextract emits a fixed cwd-relative libraryLookup; re-apply the resilient NativeLibrary lookup.
  sed -i \
    's#SymbolLookup\.libraryLookup("libhelm4j/libhelm4j\.so", LIBRARY_ARENA)#dev.nthings.helm4j.runtime.ffm.internal.NativeLibrary.symbolLookup(LIBRARY_ARENA)#' \
    helm4j-runtime-native/src/main/generated/dev/nthings/helm4j/jextract/libhelm4j_h.java

# Verify Go exports/header/jextract/bridge stay in sync
check-native-parity:
  #!/usr/bin/env bash
  set -euo pipefail
  go_exports="$(mktemp)"
  header_exports="$(mktemp)"
  jextract_exports="$(mktemp)"
  bridge_methods="$(mktemp)"
  expected_bridge_methods="$(mktemp)"
  trap 'rm -f "$go_exports" "$header_exports" "$jextract_exports" "$bridge_methods" "$expected_bridge_methods"' EXIT
  rg '^//export Helm' libhelm4j/main.go | sed -E 's#^//export (Helm[^[:space:]]+).*#\1#' | sort -u > "$go_exports"
  rg '^extern char\* Helm' libhelm4j/libhelm4j.h | sed -E 's#^extern char\* (Helm[^\(]+)\(.*#\1#' | sort -u > "$header_exports"
  rg 'public static MemorySegment Helm' helm4j-runtime-native/src/main/generated/dev/nthings/helm4j/jextract/libhelm4j_h.java | sed -E 's#^.* (Helm[^\(]+)\(.*#\1#' | grep -Fv '$' | sort -u > "$jextract_exports"
  diff -u "$go_exports" "$header_exports"
  diff -u "$go_exports" "$jextract_exports"
  while IFS= read -r symbol; do
    name="${symbol#Helm}"
    if [[ "$symbol" == "HelmPackage" ]]; then
      echo packageChart
    else
      echo "$name" | awk '{print tolower(substr($0,1,1)) substr($0,2)}'
    fi
  done < "$go_exports" | sort -u > "$expected_bridge_methods"
  rg '^[[:space:]]*byte\[]\s+[a-zA-Z][a-zA-Z0-9]*\(' helm4j-runtime-native/src/main/java/dev/nthings/helm4j/runtime/ffm/internal/HelmBridge.java | sed -E 's#^[[:space:]]*byte\[]\s+([a-zA-Z][a-zA-Z0-9]*)\(.*#\1#' | sort -u > "$bridge_methods"
  diff -u "$expected_bridge_methods" "$bridge_methods"
  echo "✓ Native API parity checks passed"
