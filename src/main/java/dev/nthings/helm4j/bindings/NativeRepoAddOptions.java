package dev.nthings.helm4j.bindings;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record NativeRepoAddOptions(
    @JsonProperty("name") String name,
    @JsonProperty("url") String url,
    @JsonProperty("username") String username,
    @JsonProperty("password") String password,
    @JsonProperty("certFile") String certificateFile,
    @JsonProperty("keyFile") String keyFile,
    @JsonProperty("caFile") String certificateAuthorityFile,
    @JsonProperty("insecureSkipTlsVerify") Boolean insecureSkipTlsVerification,
    @JsonProperty("passCredentialsAll") Boolean passCredentialsToAllHosts,
    @JsonProperty("forceUpdate") Boolean forceUpdate) {}
