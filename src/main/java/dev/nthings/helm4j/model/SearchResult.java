package dev.nthings.helm4j.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SearchResult(
    @JsonProperty("name") String name,
    @JsonProperty("version") String version,
    @JsonProperty("appVersion") String appVersion,
    @JsonProperty("description") String description,
    @JsonProperty("score") int score) {}
