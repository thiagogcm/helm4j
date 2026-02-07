.PHONY: help build test clean run docker-up docker-down docker-build shadow native coverage check

help:
	@echo "Helm4j - Available Commands:"
	@echo ""
	@echo "  make build         - Build the project"
	@echo "  make test          - Run tests"
	@echo "  make check         - Run build + tests + coverage"
	@echo "  make clean         - Clean build artifacts"
	@echo "  make run           - Run the application (requires PROMPT variable)"
	@echo "  make shadow        - Build fat JAR"
	@echo "  make native        - Build GraalVM native image"
	@echo "  make coverage      - Generate test coverage report"
	@echo "  make docker-build  - Build Docker image"
	@echo "  make docker-up     - Start services with docker-compose"
	@echo "  make docker-down   - Stop docker-compose services"
	@echo ""
	@echo "Examples:"
	@echo "  make run PROMPT='Hello from Makefile'"
	@echo "  make run PROMPT='Explain LangChain4j' MODEL=gpt-4"

build:
	./gradlew build --no-daemon

test:
	./gradlew test --no-daemon

check: build coverage
	@echo "✓ All checks passed"

clean:
	./gradlew clean --no-daemon
	rm -f *.hprof
	rm -f docker-compose.log

shadow:
	./gradlew shadowJar --no-daemon
	@echo "Fat JAR created: build/libs/helm4j-1.0-SNAPSHOT-all.jar"
native:
	./gradlew nativeCompile --no-daemon
	@echo "Native image created: build/native/nativeCompile/helm4j"

coverage:
	./gradlew jacocoTestReport --no-daemon
	@echo "Coverage report: build/reports/jacoco/test/html/index.html"

docker-build:
	docker build -t helm4j:latest .

docker-up:
	docker compose up --build

docker-down:
	docker compose down

sbom:
	./gradlew cyclonedxBom --no-daemon
	@echo "SBOM generated: build/reports/bom.json"
