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
            .repoUrl("https://example.com/charts")
            .username("demo")
            .password("secret")
            .plainHttp(true)
            .insecureSkipTlsVerify(true)
            .keyring("/tmp/keyring.gpg")
            .certFile("/tmp/cert.pem")
            .keyFile("/tmp/key.pem")
            .caFile("/tmp/ca.pem")
            .passCredentialsAll(true)
            .verify(true)
            .devel(true)
            .jsonPathTemplate("{.name}")
            .build();

    assertEquals("1.2.3", options.version());
    assertEquals("https://example.com/charts", options.repoUrl());
    assertEquals("demo", options.username());
    assertEquals("secret", options.password());
    assertTrue(options.plainHttp());
    assertTrue(options.insecureSkipTlsVerify());
    assertEquals("/tmp/keyring.gpg", options.keyring());
    assertEquals("/tmp/cert.pem", options.certFile());
    assertEquals("/tmp/key.pem", options.keyFile());
    assertEquals("/tmp/ca.pem", options.caFile());
    assertTrue(options.passCredentialsAll());
    assertTrue(options.verify());
    assertTrue(options.devel());
    assertEquals("{.name}", options.jsonPathTemplate());
  }

  @Test
  void builderDefaultsToNullValues() {
    var options = ShowOptions.builder().build();

    assertNull(options.version());
    assertNull(options.repoUrl());
    assertNull(options.username());
    assertNull(options.password());
    assertNull(options.plainHttp());
    assertNull(options.insecureSkipTlsVerify());
    assertNull(options.keyring());
    assertNull(options.certFile());
    assertNull(options.keyFile());
    assertNull(options.caFile());
    assertNull(options.passCredentialsAll());
    assertNull(options.verify());
    assertNull(options.devel());
    assertNull(options.jsonPathTemplate());
  }

  @Test
  void builderCanSetFalseFlags() {
    var options =
        ShowOptions.builder()
            .plainHttp(false)
            .insecureSkipTlsVerify(false)
            .passCredentialsAll(false)
            .verify(false)
            .devel(false)
            .build();

    assertFalse(options.plainHttp());
    assertFalse(options.insecureSkipTlsVerify());
    assertFalse(options.passCredentialsAll());
    assertFalse(options.verify());
    assertFalse(options.devel());
  }
}
