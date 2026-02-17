.PHONY: help build test clean coverage check go-build go-test go-vet go-fix go-all

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
