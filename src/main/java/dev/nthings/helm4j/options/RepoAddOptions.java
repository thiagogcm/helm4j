package dev.nthings.helm4j.options;

/** Options accepted by {@code helm repo add}. */
public record RepoAddOptions(
    String name,
    String url,
    String username,
    String password,
    String certificateFile,
    String keyFile,
    String certificateAuthorityFile,
    boolean insecureSkipTlsVerification,
    boolean passCredentialsToAllHosts,
    boolean forceUpdate) {

  public RepoAddOptions {
    name = normalize(name);
    url = normalize(url);
    username = normalize(username);
    password = normalize(password);
    certificateFile = normalize(certificateFile);
    keyFile = normalize(keyFile);
    certificateAuthorityFile = normalize(certificateAuthorityFile);
  }

  public static RepoAddOptions defaults() {
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
    private String name;
    private String url;
    private String username;
    private String password;
    private String certificateFile;
    private String keyFile;
    private String certificateAuthorityFile;
    private boolean insecureSkipTlsVerification;
    private boolean passCredentialsToAllHosts;
    private boolean forceUpdate;

    private Builder() {}

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder url(String url) {
      this.url = url;
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

    public Builder insecureSkipTlsVerification(boolean insecureSkipTlsVerification) {
      this.insecureSkipTlsVerification = insecureSkipTlsVerification;
      return this;
    }

    public Builder passCredentialsToAllHosts(boolean passCredentialsToAllHosts) {
      this.passCredentialsToAllHosts = passCredentialsToAllHosts;
      return this;
    }

    public Builder forceUpdate(boolean forceUpdate) {
      this.forceUpdate = forceUpdate;
      return this;
    }

    public RepoAddOptions build() {
      return new RepoAddOptions(
          name,
          url,
          username,
          password,
          certificateFile,
          keyFile,
          certificateAuthorityFile,
          insecureSkipTlsVerification,
          passCredentialsToAllHosts,
          forceUpdate);
    }
  }
}
