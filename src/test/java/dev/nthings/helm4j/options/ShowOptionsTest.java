package dev.nthings.helm4j.options;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShowOptionsTest {

  @Test
  void builderSetsAllFields() {
    var options =
        ShowOptions.builder()
            .version("1.2.3")
            .repositoryUrl("https://example.com/charts")
            .username("demo")
            .password("secret")
            .plainHttp(true)
            .insecureSkipTlsVerification(true)
            .keyringPath("/tmp/keyring.gpg")
            .certificateFile("/tmp/cert.pem")
            .keyFile("/tmp/key.pem")
            .certificateAuthorityFile("/tmp/ca.pem")
            .passCredentialsToAllHosts(true)
            .verifySignatures(true)
            .includePreReleaseVersions(true)
            .valuesJsonPath("{.name}")
            .build();

    assertEquals("1.2.3", options.version());
    assertEquals("https://example.com/charts", options.repositoryUrl());
    assertEquals("demo", options.username());
    assertEquals("secret", options.password());
    assertTrue(options.plainHttp());
    assertTrue(options.insecureSkipTlsVerification());
    assertEquals("/tmp/keyring.gpg", options.keyringPath());
    assertEquals("/tmp/cert.pem", options.certificateFile());
    assertEquals("/tmp/key.pem", options.keyFile());
    assertEquals("/tmp/ca.pem", options.certificateAuthorityFile());
    assertTrue(options.passCredentialsToAllHosts());
    assertTrue(options.verifySignatures());
    assertTrue(options.includePreReleaseVersions());
    assertEquals("{.name}", options.valuesJsonPath());
  }

  @Test
  void defaultsUseDeterministicBooleansAndNullStrings() {
    var options = ShowOptions.defaults();

    assertNull(options.version());
    assertNull(options.repositoryUrl());
    assertNull(options.username());
    assertNull(options.password());
    assertFalse(options.plainHttp());
    assertFalse(options.insecureSkipTlsVerification());
    assertNull(options.keyringPath());
    assertNull(options.certificateFile());
    assertNull(options.keyFile());
    assertNull(options.certificateAuthorityFile());
    assertFalse(options.passCredentialsToAllHosts());
    assertFalse(options.verifySignatures());
    assertFalse(options.includePreReleaseVersions());
    assertNull(options.valuesJsonPath());
  }

  @Test
  void builderCanSetFalseFlags() {
    var options =
        ShowOptions.builder()
            .plainHttp(false)
            .insecureSkipTlsVerification(false)
            .passCredentialsToAllHosts(false)
            .verifySignatures(false)
            .includePreReleaseVersions(false)
            .build();

    assertFalse(options.plainHttp());
    assertFalse(options.insecureSkipTlsVerification());
    assertFalse(options.passCredentialsToAllHosts());
    assertFalse(options.verifySignatures());
    assertFalse(options.includePreReleaseVersions());
  }

  @Test
  void blankStringsAreNormalizedToNull() {
    var options =
        ShowOptions.builder()
            .version("  ")
            .repositoryUrl(" ")
            .username(" ")
            .password(" ")
            .valuesJsonPath(" ")
            .build();

    assertNull(options.version());
    assertNull(options.repositoryUrl());
    assertNull(options.username());
    assertNull(options.password());
    assertNull(options.valuesJsonPath());
  }
}
