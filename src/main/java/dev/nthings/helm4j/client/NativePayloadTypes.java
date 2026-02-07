package dev.nthings.helm4j.client;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
record NativeShowOptions(
    @JsonProperty("version") String version,
    @JsonProperty("repo") String repositoryUrl,
    @JsonProperty("username") String username,
    @JsonProperty("password") String password,
    @JsonProperty("plainHttp") Boolean plainHttp,
    @JsonProperty("insecureSkipTlsVerify") Boolean insecureSkipTlsVerification,
    @JsonProperty("keyring") String keyringPath,
    @JsonProperty("certFile") String certificateFile,
    @JsonProperty("keyFile") String keyFile,
    @JsonProperty("caFile") String certificateAuthorityFile,
    @JsonProperty("passCredentialsAll") Boolean passCredentialsToAllHosts,
    @JsonProperty("verify") Boolean verifySignatures,
    @JsonProperty("devel") Boolean includePreReleaseVersions,
    @JsonProperty("jsonpath") String valuesJsonPath) {}

@JsonInclude(JsonInclude.Include.NON_NULL)
record NativeSearchOptions(
    @JsonProperty("keyword") String query,
    @JsonProperty("regexp") Boolean regularExpression,
    @JsonProperty("versions") Boolean includeAllVersions,
    @JsonProperty("devel") Boolean includePreReleaseVersions,
    @JsonProperty("version") String versionConstraint,
    @JsonProperty("failOnNoResult") Boolean failIfNoResults) {}

@JsonIgnoreProperties(ignoreUnknown = true)
record NativeShowPayload(
    @JsonProperty("mode") String mode,
    @JsonProperty("chartRef") String chartRef,
    @JsonProperty("chartPath") String chartPath,
    @JsonProperty("sections") NativeShowSections sections,
    @JsonProperty("cliOutput") String cliOutput) {}

@JsonIgnoreProperties(ignoreUnknown = true)
record NativeShowSections(
    @JsonProperty("chart") String chart,
    @JsonProperty("values") String values,
    @JsonProperty("readme") String readme,
    @JsonProperty("crds") List<String> crds) {}

@JsonIgnoreProperties(ignoreUnknown = true)
record NativeSearchPayload(@JsonProperty("results") List<NativeSearchResult> results) {}

@JsonIgnoreProperties(ignoreUnknown = true)
record NativeSearchResult(
    @JsonProperty("name") String name,
    @JsonProperty("version") String version,
    @JsonProperty("appVersion") String appVersion,
    @JsonProperty("description") String description,
    @JsonProperty("score") Integer score) {}
