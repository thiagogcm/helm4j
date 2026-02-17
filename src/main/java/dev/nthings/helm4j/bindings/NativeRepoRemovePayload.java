package dev.nthings.helm4j.bindings;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NativeRepoRemovePayload(@JsonProperty("removed") List<String> removed) {}
