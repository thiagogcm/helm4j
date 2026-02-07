package dev.nthings.helm4j.options;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Java mirror of the Go ShowOptions struct passed to the native layer. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ShowOptions {

  @JsonProperty("version")
  private final String version;

  @JsonProperty("repo")
  private final String repoUrl;

  @JsonProperty("username")
  private final String username;

  @JsonProperty("password")
  private final String password;

  @JsonProperty("plainHttp")
  private final Boolean plainHttp;

  @JsonProperty("insecureSkipTlsVerify")
  private final Boolean insecureSkipTlsVerify;

  @JsonProperty("keyring")
  private final String keyring;

  @JsonProperty("certFile")
  private final String certFile;

  @JsonProperty("keyFile")
  private final String keyFile;

  @JsonProperty("caFile")
  private final String caFile;

  @JsonProperty("passCredentialsAll")
  private final Boolean passCredentialsAll;

  @JsonProperty("verify")
  private final Boolean verify;

  @JsonProperty("devel")
  private final Boolean devel;

  @JsonProperty("jsonpath")
  private final String jsonPathTemplate;

  private ShowOptions(Builder builder) {
    this.version = builder.version;
    this.repoUrl = builder.repoUrl;
    this.username = builder.username;
    this.password = builder.password;
    this.plainHttp = builder.plainHttp;
    this.insecureSkipTlsVerify = builder.insecureSkipTlsVerify;
    this.keyring = builder.keyring;
    this.certFile = builder.certFile;
    this.keyFile = builder.keyFile;
    this.caFile = builder.caFile;
    this.passCredentialsAll = builder.passCredentialsAll;
    this.verify = builder.verify;
    this.devel = builder.devel;
    this.jsonPathTemplate = builder.jsonPathTemplate;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String version() {
    return version;
  }

  public String repoUrl() {
    return repoUrl;
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

  public Boolean insecureSkipTlsVerify() {
    return insecureSkipTlsVerify;
  }

  public String keyring() {
    return keyring;
  }

  public String certFile() {
    return certFile;
  }

  public String keyFile() {
    return keyFile;
  }

  public String caFile() {
    return caFile;
  }

  public Boolean passCredentialsAll() {
    return passCredentialsAll;
  }

  public Boolean verify() {
    return verify;
  }

  public Boolean devel() {
    return devel;
  }

  public String jsonPathTemplate() {
    return jsonPathTemplate;
  }

  public static final class Builder {
    private String version;
    private String repoUrl;
    private String username;
    private String password;
    private Boolean plainHttp;
    private Boolean insecureSkipTlsVerify;
    private String keyring;
    private String certFile;
    private String keyFile;
    private String caFile;
    private Boolean passCredentialsAll;
    private Boolean verify;
    private Boolean devel;
    private String jsonPathTemplate;

    private Builder() {}

    public Builder version(String version) {
      this.version = version;
      return this;
    }

    public Builder repoUrl(String repoUrl) {
      this.repoUrl = repoUrl;
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

    public Builder insecureSkipTlsVerify(boolean insecureSkipTlsVerify) {
      this.insecureSkipTlsVerify = insecureSkipTlsVerify;
      return this;
    }

    public Builder keyring(String keyring) {
      this.keyring = keyring;
      return this;
    }

    public Builder certFile(String certFile) {
      this.certFile = certFile;
      return this;
    }

    public Builder keyFile(String keyFile) {
      this.keyFile = keyFile;
      return this;
    }

    public Builder caFile(String caFile) {
      this.caFile = caFile;
      return this;
    }

    public Builder passCredentialsAll(boolean passCredentialsAll) {
      this.passCredentialsAll = passCredentialsAll;
      return this;
    }

    public Builder verify(boolean verify) {
      this.verify = verify;
      return this;
    }

    public Builder devel(boolean devel) {
      this.devel = devel;
      return this;
    }

    public Builder jsonPathTemplate(String jsonPathTemplate) {
      this.jsonPathTemplate = jsonPathTemplate;
      return this;
    }

    public ShowOptions build() {
      return new ShowOptions(this);
    }
  }
}
