package dev.nthings.helm4j.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ShowResponse(
    @JsonProperty("mode") ShowMode mode,
    @JsonProperty("chartRef") String chartRef,
    @JsonProperty("chartPath") String chartPath,
    @JsonProperty("sections") ShowSections sections,
    @JsonProperty("cliOutput") String cliOutput) {}
