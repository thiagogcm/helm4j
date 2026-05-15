package dev.nthings.helm4j.runtime.ffm.internal;

import java.util.List;

/** JSON-bridge representation of a release hook. */
record HookPayload(String name, String kind, String path, List<String> events, int weight) {}
