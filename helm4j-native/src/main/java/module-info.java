/** Native runtime: FFM bridge to libhelm4j and the JSON gateway implementation. */
module dev.nthings.helm4j.runtime {
  requires dev.nthings.helm4j;
  requires tools.jackson.core;
  requires tools.jackson.databind;
  requires org.slf4j;

  provides dev.nthings.helm4j.internal.spi.HelmGatewayProvider with
      dev.nthings.helm4j.internal.runtime.FfmHelmGatewayProvider;
}
