/** Default FFM-backed Helm4j runtime provider; delegates to libhelm4j via jextract. */
module dev.nthings.helm4j.runtime.ffm {
  requires dev.nthings.helm4j.spi;
  requires tools.jackson.core;
  requires tools.jackson.databind;
  requires org.slf4j;

  // Jackson reflectively (de)serializes the gateway's internal JSON-bridge payload records.
  opens dev.nthings.helm4j.runtime.ffm.internal to
      tools.jackson.databind;

  provides dev.nthings.helm4j.spi.HelmEngineProvider with
      dev.nthings.helm4j.runtime.ffm.FfmHelmEngineProvider;
}
