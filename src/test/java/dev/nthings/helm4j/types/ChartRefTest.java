package dev.nthings.helm4j.types;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChartRefTest {

  @Test
  void chartReferenceFactoryMethodsReturnTypedReferences() {
    var repo = ChartRef.repo("bitnami/nginx");
    var oci = ChartRef.oci("oci://registry-1.docker.io/bitnamicharts/nginx");
    var local = ChartRef.local(Path.of("charts", "hello"));

    assertEquals("bitnami/nginx", repo.asReference());
    assertEquals("oci://registry-1.docker.io/bitnamicharts/nginx", oci.asReference());
    assertEquals(
        Path.of("charts", "hello").toAbsolutePath().normalize().toString(), local.asReference());
  }

  @Test
  void ociReferencesMustStartWithOciPrefix() {
    assertThrows(IllegalArgumentException.class, () -> ChartRef.oci("docker.io/bitnami/nginx"));
  }
}
