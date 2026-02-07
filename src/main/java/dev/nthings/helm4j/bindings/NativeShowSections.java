package dev.nthings.helm4j.bindings;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NativeShowSections(
    @JsonProperty("chart") String chart,
    @JsonProperty("values") String values,
    @JsonProperty("readme") String readme,
    @JsonProperty("crds") List<String> crds) {}
