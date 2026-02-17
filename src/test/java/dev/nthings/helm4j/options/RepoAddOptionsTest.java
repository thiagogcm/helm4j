package dev.nthings.helm4j.options;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepoAddOptionsTest {

  @Test
  void builderSetsAllFields() {
    var options =
        RepoAddOptions.builder()
            .name("bitnami")
            .url("https://charts.bitnami.com/bitnami")
            .username("demo")
            .password("secret")
            .certificateFile("/tmp/cert.pem")
            .keyFile("/tmp/key.pem")
            .certificateAuthorityFile("/tmp/ca.pem")
            .insecureSkipTlsVerification(true)
            .passCredentialsToAllHosts(true)
            .forceUpdate(true)
            .build();

    assertEquals("bitnami", options.name());
    assertEquals("https://charts.bitnami.com/bitnami", options.url());
    assertEquals("demo", options.username());
    assertEquals("secret", options.password());
    assertEquals("/tmp/cert.pem", options.certificateFile());
    assertEquals("/tmp/key.pem", options.keyFile());
    assertEquals("/tmp/ca.pem", options.certificateAuthorityFile());
    assertTrue(options.insecureSkipTlsVerification());
    assertTrue(options.passCredentialsToAllHosts());
    assertTrue(options.forceUpdate());
  }

  @Test
  void defaultsUseDeterministicBooleansAndNullStrings() {
    var options = RepoAddOptions.defaults();

    assertNull(options.name());
    assertNull(options.url());
    assertNull(options.username());
    assertNull(options.password());
    assertNull(options.certificateFile());
    assertNull(options.keyFile());
    assertNull(options.certificateAuthorityFile());
    assertFalse(options.insecureSkipTlsVerification());
    assertFalse(options.passCredentialsToAllHosts());
    assertFalse(options.forceUpdate());
  }

  @Test
  void blankStringsAreNormalizedToNull() {
    var options =
        RepoAddOptions.builder()
            .name(" ")
            .url(" ")
            .username(" ")
            .password(" ")
            .certificateFile(" ")
            .keyFile(" ")
            .certificateAuthorityFile(" ")
            .build();

    assertNull(options.name());
    assertNull(options.url());
    assertNull(options.username());
    assertNull(options.password());
    assertNull(options.certificateFile());
    assertNull(options.keyFile());
    assertNull(options.certificateAuthorityFile());
  }
}
