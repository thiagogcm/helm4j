package dev.nthings.helm4j.bindings;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record NativeShowOptions(
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
