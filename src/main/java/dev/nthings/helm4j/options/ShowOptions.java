package dev.nthings.helm4j.options;

/** Options accepted by {@code helm show}. */
public record ShowOptions(
    String version,
    String repositoryUrl,
    String username,
    String password,
    boolean plainHttp,
    boolean insecureSkipTlsVerification,
    String keyringPath,
    String certificateFile,
    String keyFile,
    String certificateAuthorityFile,
    boolean passCredentialsToAllHosts,
    boolean verifySignatures,
    boolean includePreReleaseVersions,
    String valuesJsonPath) {

  public ShowOptions {
    version = normalize(version);
    repositoryUrl = normalize(repositoryUrl);
    username = normalize(username);
    password = normalize(password);
    keyringPath = normalize(keyringPath);
    certificateFile = normalize(certificateFile);
    keyFile = normalize(keyFile);
    certificateAuthorityFile = normalize(certificateAuthorityFile);
    valuesJsonPath = normalize(valuesJsonPath);
  }

  public static ShowOptions defaults() {
    return builder().build();
  }

  public static Builder builder() {
    return new Builder();
  }

  private static String normalize(String value) {
    if (value == null) {
      return null;
    }
    var trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  public static final class Builder {
    private String version;
    private String repositoryUrl;
    private String username;
    private String password;
    private boolean plainHttp;
    private boolean insecureSkipTlsVerification;
    private String keyringPath;
    private String certificateFile;
    private String keyFile;
    private String certificateAuthorityFile;
    private boolean passCredentialsToAllHosts;
    private boolean verifySignatures;
    private boolean includePreReleaseVersions;
    private String valuesJsonPath;

    private Builder() {}

    public Builder version(String version) {
      this.version = version;
      return this;
    }

    public Builder repositoryUrl(String repositoryUrl) {
      this.repositoryUrl = repositoryUrl;
      return this;
    }

    public Builder username(String username) {
      this.username = username;
      return this;
    }

    public Builder password(String password) {
      this.password = password;
      return this;
    }

    public Builder plainHttp(boolean plainHttp) {
      this.plainHttp = plainHttp;
      return this;
    }

    public Builder insecureSkipTlsVerification(boolean insecureSkipTlsVerification) {
      this.insecureSkipTlsVerification = insecureSkipTlsVerification;
      return this;
    }

    public Builder keyringPath(String keyringPath) {
      this.keyringPath = keyringPath;
      return this;
    }

    public Builder certificateFile(String certificateFile) {
      this.certificateFile = certificateFile;
      return this;
    }

    public Builder keyFile(String keyFile) {
      this.keyFile = keyFile;
      return this;
    }

    public Builder certificateAuthorityFile(String certificateAuthorityFile) {
      this.certificateAuthorityFile = certificateAuthorityFile;
      return this;
    }

    public Builder passCredentialsToAllHosts(boolean passCredentialsToAllHosts) {
      this.passCredentialsToAllHosts = passCredentialsToAllHosts;
      return this;
    }

    public Builder verifySignatures(boolean verifySignatures) {
      this.verifySignatures = verifySignatures;
      return this;
    }

    public Builder includePreReleaseVersions(boolean includePreReleaseVersions) {
      this.includePreReleaseVersions = includePreReleaseVersions;
      return this;
    }

    public Builder valuesJsonPath(String valuesJsonPath) {
      this.valuesJsonPath = valuesJsonPath;
      return this;
    }

    public ShowOptions build() {
      return new ShowOptions(
          version,
          repositoryUrl,
          username,
          password,
          plainHttp,
          insecureSkipTlsVerification,
          keyringPath,
          certificateFile,
          keyFile,
          certificateAuthorityFile,
          passCredentialsToAllHosts,
          verifySignatures,
          includePreReleaseVersions,
          valuesJsonPath);
    }
  }
}
