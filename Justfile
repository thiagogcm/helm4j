# helm4j dev workflow. Run `just` to list recipes.
#
# Requires: just >= 1.40, JDK 25, Go (see libhelm4j/go.mod), ripgrep (rg).
#   - jextract additionally needs LLVM_HOME and `jextract` on PATH.
#   - dev recipes (dev-env, typecheck) need Bun.

# ── Configuration (single source of truth) ────────────────────────────────────
gradle := "./gradlew --no-daemon" # flip --no-daemon here to affect every gradle recipe
go := "go"
lib_dir := "libhelm4j"
lib_so := lib_dir / "libhelm4j.so"
lib_header := lib_dir / "libhelm4j.h"
pkg := "dev.nthings.helm4j"
native_module := "helm4j-runtime-native"
generated := native_module / "src/main/generated"
jextract_h := generated / "dev/nthings/helm4j/jextract/libhelm4j_h.java"
bridge := native_module / "src/main/java/dev/nthings/helm4j/runtime/ffm/internal/HelmBridge.java"

# List available recipes (grouped).
help:
    @just --list

# ── Build ─────────────────────────────────────────────────────────────────────

# Build everything (Go shared library + Java).
[group('build')]
build: go-build
    {{ gradle }} build

# Remove all build artifacts (Gradle outputs, native library, heap dumps).
[group('build')]
clean:
    {{ gradle }} clean
    rm -f {{ lib_so }} {{ lib_header }} *.hprof

# ── Test & coverage ───────────────────────────────────────────────────────────

# Run Java tests.
[group('test')]
test:
    {{ gradle }} test

# Run Java tests + JaCoCo coverage report + threshold verification.
[group('test')]
coverage:
    {{ gradle }} test jacocoTestReport jacocoTestCoverageVerification
    @echo "Coverage reports: helm4j-*/build/reports/jacoco/test/html/index.html"

# Full verification: Go pipeline, native parity, Java build, coverage.
[group('test')]
check: go-all check-native-parity build coverage
    @echo "✓ All checks passed"

# Alias used by CI so dev and CI share one entry point.
alias ci := check

# ── Go ────────────────────────────────────────────────────────────────────────

# Build the Go c-shared library (libhelm4j.so).
[group('go')]
[working-directory('libhelm4j')]
go-build:
    CGO_ENABLED=1 {{ go }} build -buildmode=c-shared -trimpath -ldflags="-s -w -extldflags=-Wl,--version-script=$PWD/libhelm4j.map" -o libhelm4j.so .

# Run Go unit tests.
[group('go')]
[working-directory('libhelm4j')]
go-test:
    {{ go }} test -v -count=1 ./internal/...

# Run go vet.
[group('go')]
[working-directory('libhelm4j')]
go-vet:
    {{ go }} vet ./...

# Apply Go modernisers (go fix, Go 1.26+).
[group('go')]
[working-directory('libhelm4j')]
go-fix:
    {{ go }} fix ./...

# Format Go sources.
[group('go')]
[working-directory('libhelm4j')]
go-fmt:
    gofmt -w .

# go vet + go test + go build.
[group('go')]
go-all: go-vet go-test go-build
    @echo "✓ Go pipeline passed"

# ── Native bindings ───────────────────────────────────────────────────────────

# Regenerate Java FFM bindings from the C header (requires LLVM_HOME + jextract).
[group('native')]
jextract: go-build
    #!/usr/bin/env bash
    set -euo pipefail
    : "${LLVM_HOME:?LLVM_HOME is required (example: export LLVM_HOME=/usr/lib/llvm-18)}"
    # Single source of truth: derive the binding set from the Go //export directives,
    # the same chain `check-native-parity` validates end to end.
    includes=()
    while IFS= read -r fn; do
      includes+=(--include-function "$fn")
    done < <(rg -N '^//export ' {{ lib_dir }}/main.go | sed -E 's#^//export ([A-Za-z0-9_]+).*#\1#')
    LD_LIBRARY_PATH="${LLVM_HOME}/lib" jextract \
      -Djava.library.path="${LLVM_HOME}" \
      -I{{ lib_dir }} -l:{{ lib_so }} \
      "${includes[@]}" \
      --output {{ generated }} --target-package {{ pkg }}.jextract {{ lib_header }}
    # jextract emits a fixed cwd-relative libraryLookup; re-apply the resilient NativeLibrary lookup.
    sed -i \
      's#SymbolLookup\.libraryLookup("libhelm4j/libhelm4j\.so", LIBRARY_ARENA)#{{ pkg }}.runtime.ffm.internal.NativeLibrary.symbolLookup(LIBRARY_ARENA)#' \
      {{ jextract_h }}

# Verify Go exports / header / jextract bindings / bridge stay in sync.
[group('native')]
check-native-parity:
    #!/usr/bin/env bash
    set -euo pipefail
    go_exports="$(mktemp)"
    header_exports="$(mktemp)"
    jextract_exports="$(mktemp)"
    bridge_methods="$(mktemp)"
    expected_bridge_methods="$(mktemp)"
    trap 'rm -f "$go_exports" "$header_exports" "$jextract_exports" "$bridge_methods" "$expected_bridge_methods"' EXIT
    rg '^//export Helm' {{ lib_dir }}/main.go | sed -E 's#^//export (Helm[^[:space:]]+).*#\1#' | sort -u > "$go_exports"
    rg '^extern char\* Helm' {{ lib_header }} | sed -E 's#^extern char\* (Helm[^\(]+)\(.*#\1#' | sort -u > "$header_exports"
    rg 'public static MemorySegment Helm' {{ jextract_h }} | sed -E 's#^.* (Helm[^\(]+)\(.*#\1#' | grep -Fv '$' | sort -u > "$jextract_exports"
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
    rg '^[[:space:]]*byte\[]\s+[a-zA-Z][a-zA-Z0-9]*\(' {{ bridge }} | sed -E 's#^[[:space:]]*byte\[]\s+([a-zA-Z][a-zA-Z0-9]*)\(.*#\1#' | sort -u > "$bridge_methods"
    diff -u "$expected_bridge_methods" "$bridge_methods"
    echo "✓ Native API parity checks passed"

# ── Dev ───────────────────────────────────────────────────────────────────────

# Format Go, Java, and this Justfile.
[group('dev')]
fmt: go-fmt
    {{ gradle }} spotlessApply
    just --fmt
    @echo "✓ Formatted Go, Java, and Justfile"

# Spin up the local Kind dev cluster (WSL2-aware).
[group('dev')]
dev-env:
    bun run scripts/dev-env.ts

# Type-check the Bun/TypeScript scripts.
[group('dev')]
typecheck:
    bun run typecheck

# Report the resolved path of each required tool (fails if one is missing).
[group('dev')]
tools:
    @echo "go:   {{ require('go') }}"
    @echo "java: {{ require('java') }}"
    @echo "rg:   {{ require('rg') }}"
    @echo "bun:  {{ require('bun') }}"
