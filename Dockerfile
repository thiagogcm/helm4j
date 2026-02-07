# Multi-stage build for optimized image size
FROM eclipse-temurin:25.0.2_10-jdk-alpine AS build

ARG VERSION=1.0-SNAPSHOT

WORKDIR /app

# Copy Gradle wrapper and build files
COPY gradle gradle
COPY gradlew .
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY gradle/libs.versions.toml gradle/

# Download dependencies (cached layer)
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew dependencies --no-daemon

# Copy source code
COPY src src

# Build the application
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew shadowJar --no-daemon

# Runtime stage with minimal image
FROM eclipse-temurin:25.0.2_10-jre-alpine

ARG VERSION=1.0-SNAPSHOT

# Add OCI labels
LABEL org.opencontainers.image.title="helm4j"
LABEL org.opencontainers.image.description="Helm4j Application"
LABEL org.opencontainers.image.version="${VERSION}"
LABEL org.opencontainers.image.vendor="NThings"

WORKDIR /app

# Install tzdata for timezone support
RUN apk add --no-cache tzdata

# Copy the fat jar from build stage
COPY --from=build /app/build/libs/helm4j-${VERSION}-all.jar app.jar

# Create non-root user
RUN addgroup -g 1000 appuser && \
    adduser -D -u 1000 -G appuser appuser && \
    chown -R appuser:appuser /app

USER appuser

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
