package dev.nthings.helm4j.options;

/** Options accepted by {@code helm show}. */
public final class ShowOptions {

  private final String version;
  private final String repositoryUrl;
  private final String username;
  private final String password;
  private final Boolean plainHttp;
  private final Boolean insecureSkipTlsVerification;
  private final String keyringPath;
  private final String certificateFile;
  private final String keyFile;
  private final String certificateAuthorityFile;
  private final Boolean passCredentialsToAllHosts;
  private final Boolean verifySignatures;
  private final Boolean includePreReleaseVersions;
  private final String valuesJsonPath;

  private ShowOptions(Builder builder) {
    this.version = builder.version;
    this.repositoryUrl = builder.repositoryUrl;
    this.username = builder.username;
    this.password = builder.password;
    this.plainHttp = builder.plainHttp;
    this.insecureSkipTlsVerification = builder.insecureSkipTlsVerification;
    this.keyringPath = builder.keyringPath;
    this.certificateFile = builder.certificateFile;
    this.keyFile = builder.keyFile;
    this.certificateAuthorityFile = builder.certificateAuthorityFile;
    this.passCredentialsToAllHosts = builder.passCredentialsToAllHosts;
    this.verifySignatures = builder.verifySignatures;
    this.includePreReleaseVersions = builder.includePreReleaseVersions;
    this.valuesJsonPath = builder.valuesJsonPath;
  }

  public static ShowOptions defaults() {
    return builder().build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public String version() {
    return version;
  }

  public String repositoryUrl() {
    return repositoryUrl;
  }

  public String username() {
    return username;
  }

  public String password() {
    return password;
  }

  public Boolean plainHttp() {
    return plainHttp;
  }

  public Boolean insecureSkipTlsVerification() {
    return insecureSkipTlsVerification;
  }

  public String keyringPath() {
    return keyringPath;
  }

  public String certificateFile() {
    return certificateFile;
  }

  public String keyFile() {
    return keyFile;
  }

  public String certificateAuthorityFile() {
    return certificateAuthorityFile;
  }

  public Boolean passCredentialsToAllHosts() {
    return passCredentialsToAllHosts;
  }

  public Boolean verifySignatures() {
    return verifySignatures;
  }

  public Boolean includePreReleaseVersions() {
    return includePreReleaseVersions;
  }

  public String valuesJsonPath() {
    return valuesJsonPath;
  }

  public static final class Builder {
    private String version;
    private String repositoryUrl;
    private String username;
    private String password;
    private Boolean plainHttp;
    private Boolean insecureSkipTlsVerification;
    private String keyringPath;
    private String certificateFile;
    private String keyFile;
    private String certificateAuthorityFile;
    private Boolean passCredentialsToAllHosts;
    private Boolean verifySignatures;
    private Boolean includePreReleaseVersions;
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
      return new ShowOptions(this);
    }
  }
}
