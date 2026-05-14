package dev.nthings.helm4j.chart;

import dev.nthings.helm4j.internal.api.Invocations;
import dev.nthings.helm4j.internal.gateway.ChartGateway;
import dev.nthings.helm4j.internal.model.ModelSupport;

/** Request parameters for pushing a packaged chart to an OCI registry. */
public record PushRequest(
    String chartReference,
    String remote,
    boolean plainHttp,
    boolean insecureSkipTlsVerification,
    String certificateFile,
    String keyFile,
    String certificateAuthorityFile) {

  public PushRequest {
    chartReference = ModelSupport.normalizeBlankToNull(chartReference);
    remote = ModelSupport.normalizeBlankToNull(remote);
    certificateFile = ModelSupport.normalizeBlankToNull(certificateFile);
    keyFile = ModelSupport.normalizeBlankToNull(keyFile);
    certificateAuthorityFile = ModelSupport.normalizeBlankToNull(certificateAuthorityFile);
  }

  public static Builder builder() {
    return new Builder(null);
  }

  static Builder builder(ChartGateway gateway) {
    return new Builder(gateway);
  }

  public static final class Builder {
    private final ChartGateway gateway;
    private String chartReference;
    private String remote;
    private boolean plainHttp;
    private boolean insecureSkipTlsVerification;
    private String certificateFile;
    private String keyFile;
    private String certificateAuthorityFile;

    private Builder(ChartGateway gateway) {
      this.gateway = gateway;
    }

    public Builder chartReference(String value) {
      this.chartReference = value;
      return this;
    }

    public Builder remote(String value) {
      this.remote = value;
      return this;
    }

    public Builder plainHttp(boolean value) {
      this.plainHttp = value;
      return this;
    }

    public Builder insecureSkipTlsVerification(boolean value) {
      this.insecureSkipTlsVerification = value;
      return this;
    }

    public Builder certificateFile(String value) {
      this.certificateFile = value;
      return this;
    }

    public Builder keyFile(String value) {
      this.keyFile = value;
      return this;
    }

    public Builder certificateAuthorityFile(String value) {
      this.certificateAuthorityFile = value;
      return this;
    }

    public PushRequest build() {
      return new PushRequest(
          chartReference,
          remote,
          plainHttp,
          insecureSkipTlsVerification,
          certificateFile,
          keyFile,
          certificateAuthorityFile);
    }

    /** Builds the request and pushes the chart through the bound client. */
    public PushResult execute() {
      return Invocations.requireBound(gateway).push(build());
    }
  }
}
