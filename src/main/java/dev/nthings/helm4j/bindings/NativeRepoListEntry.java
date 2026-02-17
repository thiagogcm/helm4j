package dev.nthings.helm4j.bindings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NativeRepoListEntry(
    @JsonProperty("name") String name, @JsonProperty("url") String url) {}
