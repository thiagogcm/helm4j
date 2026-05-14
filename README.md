# Helm4j

Helm4j is a Java SDK for Helm v4 focused on idiomatic Java APIs and a stable native bridge.

## Requirements

- Java 25+
- Go 1.26+
- Helm v4 SDK (bundled via `libhelm4j/go.mod`)

## Build

Development tasks are exposed through the root `Justfile`.

```bash
just --list
```

### Build Native Library

```bash
just go-build
```

### Build Java SDK

```bash
just build
```

### Run Checks

```bash
just check
```

## Highlights

## Quick Start
