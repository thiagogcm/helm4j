.PHONY: help build test clean coverage check go-build go-test go-vet go-fix go-all jextract

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

# --- Go targets ---

go-build:
	cd $(GO_MOD_DIR) && CGO_ENABLED=1 go build -buildmode=c-shared -o libhelm4j.so .

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
		--include-function HelmTemplate \
		--include-function HelmLint \
		--include-function HelmVersion \
		--output src/main/generated --target-package dev.nthings.helm4j.jextract libhelm4j/libhelm4j.h

# --- Java / Gradle targets ---

build: go-build
	./gradlew build --no-daemon

test:
	./gradlew test --no-daemon

check: go-all build coverage
	@echo "✓ All checks passed"

clean:
	./gradlew clean --no-daemon
	rm -f $(GO_LIB) $(GO_HEADER) *.hprof

coverage:
	./gradlew test jacocoTestReport jacocoTestCoverageVerification --no-daemon
	@echo "Coverage report: build/reports/jacoco/test/html/index.html"
