.PHONY: help build test clean coverage check

help:
	@echo "Helm4j - Available Commands:"
	@echo ""
	@echo "  make build         - Build the project"
	@echo "  make test          - Run tests"
	@echo "  make check         - Run build + tests + coverage"
	@echo "  make clean         - Clean build artifacts"
	@echo "  make coverage      - Generate test coverage report"

build:
	./gradlew build --no-daemon

test:
	./gradlew test --no-daemon

check: build coverage
	@echo "✓ All checks passed"

clean:
	./gradlew clean --no-daemon
	rm -f *.hprof

coverage:
	./gradlew test jacocoTestReport jacocoTestCoverageVerification --no-daemon
	@echo "Coverage report: build/reports/jacoco/test/html/index.html"
