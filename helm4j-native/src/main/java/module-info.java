/** Native runtime: FFM bridge to libhelm4j and the JSON gateway implementation. */
module dev.nthings.helm4j.runtime {
  requires dev.nthings.helm4j;
  requires tools.jackson.core;
  requires tools.jackson.databind;
  requires org.slf4j;

  // Jackson reflectively (de)serializes the gateway's internal JSON-bridge payload records.
  opens dev.nthings.helm4j.internal.runtime to
      tools.jackson.databind;

  provides dev.nthings.helm4j.internal.gateway.HelmGatewayProvider with
      dev.nthings.helm4j.internal.runtime.FfmHelmGatewayProvider;
}
