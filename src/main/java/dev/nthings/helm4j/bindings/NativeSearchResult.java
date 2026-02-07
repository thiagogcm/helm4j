package dev.nthings.helm4j.bindings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NativeSearchResult(
    @JsonProperty("name") String name,
    @JsonProperty("version") String version,
    @JsonProperty("appVersion") String appVersion,
    @JsonProperty("description") String description,
    @JsonProperty("score") Integer score) {}
