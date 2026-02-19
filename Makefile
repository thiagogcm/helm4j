.PHONY: help build test clean coverage check go-build go-test go-vet go-fix go-all jextract check-native-parity

# Go module location
GO_MOD_DIR := libhelm4j
GO_LIB     := $(GO_MOD_DIR)/libhelm4j.so
GO_HEADER  := $(GO_MOD_DIR)/libhelm4j.h

help:
	@echo "Helm4j - Available Commands:"
	@echo ""
	@echo "  make build         - Build the full project (Go + Java)"
	@echo "  make test          - Run Java tests"
	@echo "  make check         - Run build + tests + coverage"
	@echo "  make clean         - Clean all build artifacts"
	@echo "  make coverage      - Generate Java test coverage report"
	@echo ""
	@echo "  make go-build      - Build the Go shared library"
	@echo "  make go-test       - Run Go unit tests"
	@echo "  make go-vet        - Run go vet on the Go module"
	@echo "  make go-fix        - Run go fix modernisers (Go 1.26+)"
	@echo "  make go-all        - go-vet + go-test + go-build"
	@echo "  make jextract      - Regenerate Java FFM bindings from libhelm4j.h"
	@echo "  make check-native-parity - Verify Go exports/header/jextract/bridge stay in sync"

# --- Go targets ---

go-build:
	cd $(GO_MOD_DIR) && CGO_ENABLED=1 go build -buildmode=c-shared -trimpath \
		-ldflags="-s -w -extldflags=-Wl,--version-script=$$PWD/libhelm4j.map" \
		-o libhelm4j.so .

go-test:
	cd $(GO_MOD_DIR) && go test -v -count=1 ./internal/...

go-vet:
	cd $(GO_MOD_DIR) && go vet ./...

go-fix:
	cd $(GO_MOD_DIR) && go fix ./...

go-all: go-vet go-test go-build
	@echo "✓ Go pipeline passed"

jextract: go-build
	@if [ -z "$$LLVM_HOME" ]; then \
		echo "LLVM_HOME is required (example: export LLVM_HOME=/usr/lib/llvm-18)"; \
		exit 1; \
	fi
	@LD_LIBRARY_PATH=$$LLVM_HOME/lib jextract -Djava.library.path=$$LLVM_HOME -Ilibhelm4j -l:libhelm4j/libhelm4j.so \
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
		--output src/main/generated --target-package dev.nthings.helm4j.jextract libhelm4j/libhelm4j.h

check-native-parity:
	@set -eu; \
		go_exports=$$(mktemp); \
		header_exports=$$(mktemp); \
		jextract_exports=$$(mktemp); \
		bridge_methods=$$(mktemp); \
		expected_bridge_methods=$$(mktemp); \
		trap 'rm -f "$$go_exports" "$$header_exports" "$$jextract_exports" "$$bridge_methods" "$$expected_bridge_methods"' EXIT; \
		rg '^//export Helm' libhelm4j/main.go | sed -E 's#^//export (Helm[^[:space:]]+).*#\1#' | sort -u > "$$go_exports"; \
		rg '^extern char\* Helm' libhelm4j/libhelm4j.h | sed -E 's#^extern char\* (Helm[^\(]+)\(.*#\1#' | sort -u > "$$header_exports"; \
		rg 'public static MemorySegment Helm' src/main/generated/dev/nthings/helm4j/jextract/libhelm4j_h.java | sed -E 's#^.* (Helm[^\(]+)\(.*#\1#' | grep -Fv '$$' | sort -u > "$$jextract_exports"; \
		diff -u "$$go_exports" "$$header_exports"; \
		diff -u "$$go_exports" "$$jextract_exports"; \
		while IFS= read -r symbol; do \
			name="$${symbol#Helm}"; \
			if [ "$$symbol" = "HelmPackage" ]; then \
				echo packageChart; \
			else \
				echo "$$name" | awk '{print tolower(substr($$0,1,1)) substr($$0,2)}'; \
			fi; \
		done < "$$go_exports" | sort -u > "$$expected_bridge_methods"; \
		rg '^[[:space:]]*byte\[]\s+[a-zA-Z][a-zA-Z0-9]*\(' src/main/java/dev/nthings/helm4j/internal/sdk/HelmBridge.java | sed -E 's#^[[:space:]]*byte\[]\s+([a-zA-Z][a-zA-Z0-9]*)\(.*#\1#' | sort -u > "$$bridge_methods"; \
		diff -u "$$expected_bridge_methods" "$$bridge_methods"; \
		echo "✓ Native API parity checks passed"

# --- Java / Gradle targets ---

build: go-build
	./gradlew build --no-daemon

test:
	./gradlew test --no-daemon

check: go-all check-native-parity build coverage
	@echo "✓ All checks passed"

clean:
	./gradlew clean --no-daemon
	rm -f $(GO_LIB) $(GO_HEADER) *.hprof

coverage:
	./gradlew test jacocoTestReport jacocoTestCoverageVerification --no-daemon
	@echo "Coverage report: build/reports/jacoco/test/html/index.html"
