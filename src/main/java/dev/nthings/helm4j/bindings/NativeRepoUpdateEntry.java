package dev.nthings.helm4j.bindings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NativeRepoUpdateEntry(
    @JsonProperty("name") String name, @JsonProperty("status") String status) {}
