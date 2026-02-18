package dev.nthings.helm4j.chart;

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
    chartReference = normalize(chartReference);
    remote = normalize(remote);
    certificateFile = normalize(certificateFile);
    keyFile = normalize(keyFile);
    certificateAuthorityFile = normalize(certificateAuthorityFile);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private String chartReference;
    private String remote;
    private boolean plainHttp;
    private boolean insecureSkipTlsVerification;
    private String certificateFile;
    private String keyFile;
    private String certificateAuthorityFile;

    private Builder() {}

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
  }

  private static String normalize(String value) {
    if (value == null) {
      return null;
    }
    var normalized = value.trim();
    return normalized.isEmpty() ? null : normalized;
  }
}
