---
title: "Helm4j"
description: "Experimental Java API for managing Helm 4 charts and releases"
tags: [ "Java", "Helm", "Kubernetes" ]
---

## What it does

Helm4j lets Java applications work with Helm 4 through typed requests and results instead of shell commands. It is intended for services, developer tools, and automation that need Helm workflows as part of the application itself.

The API covers:

- installing, upgrading, rolling back, testing, listing, and uninstalling releases;
- reading release values, manifests, hooks, notes, and metadata;
- searching, inspecting, templating, linting, pulling, pushing, and packaging charts;
- managing chart repositories and OCI registry sessions; and
- checking the active Helm version.

## Design choices

**Java-first API:** Operations use structured request and result types, making Helm workflows easier to compose, validate, and test in Java applications.

**Helm semantics:** Helm4j follows Helm's concepts and behavior rather than introducing a separate release or chart model.

**Application-focused scope:** The project concentrates on Helm capabilities useful inside applications. Shell-oriented features such as command completion are intentionally out of scope.

## Current limitations

Helm4j is experimental and has not been thoroughly tested. Its API, behavior, and compatibility may change, so it should be evaluated carefully before production use.

- Artifacts are not published and must currently be built from source.
- The available runtime supports Linux only and requires Java 25+.
- Compatibility currently targets Helm v4.
- Selecting a Kubernetes context through the client is not yet supported; operations use the normal active context.
- Chart dependency support is limited to listing dependencies.
- Chart creation and verification, repository indexing, and Helm plugins are not exposed.
