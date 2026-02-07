package dev.nthings.helm4j.bindings;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record NativeSearchOptions(
    @JsonProperty("keyword") String query,
    @JsonProperty("regexp") Boolean regularExpression,
    @JsonProperty("versions") Boolean includeAllVersions,
    @JsonProperty("devel") Boolean includePreReleaseVersions,
    @JsonProperty("version") String versionConstraint,
    @JsonProperty("failOnNoResult") Boolean failIfNoResults) {}
