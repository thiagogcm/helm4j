package dev.nthings.helm4j.bindings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NativeShowPayload(
    @JsonProperty("mode") String mode,
    @JsonProperty("chartRef") String chartRef,
    @JsonProperty("chartPath") String chartPath,
    @JsonProperty("sections") NativeShowSections sections,
    @JsonProperty("cliOutput") String cliOutput) {}
