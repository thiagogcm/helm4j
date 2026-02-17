package dev.nthings.helm4j.options;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class RepoListOptionsTest {

  @Test
  void defaultsProduceANonNullOptionsObject() {
    assertNotNull(RepoListOptions.defaults());
  }
}
