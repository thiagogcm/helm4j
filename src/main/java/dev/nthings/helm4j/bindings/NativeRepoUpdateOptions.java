package dev.nthings.helm4j.bindings;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record NativeRepoUpdateOptions(@JsonProperty("names") List<String> names) {}
