package dev.nthings.helm4j.options;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Options accepted by {@code helm search repo}. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class SearchOptions {

  @JsonProperty("keyword")
  private final String keyword;

  @JsonProperty("regexp")
  private final Boolean regexp;

  @JsonProperty("versions")
  private final Boolean versions;

  @JsonProperty("devel")
  private final Boolean devel;

  @JsonProperty("version")
  private final String version;

  @JsonProperty("failOnNoResult")
  private final Boolean failOnNoResult;

  private SearchOptions(Builder builder) {
    this.keyword = builder.keyword;
    this.regexp = builder.regexp;
    this.versions = builder.versions;
    this.devel = builder.devel;
    this.version = builder.version;
    this.failOnNoResult = builder.failOnNoResult;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String keyword() {
    return keyword;
  }

  public Boolean regexp() {
    return regexp;
  }

  public Boolean versions() {
    return versions;
  }

  public Boolean devel() {
    return devel;
  }

  public String version() {
    return version;
  }

  public Boolean failOnNoResult() {
    return failOnNoResult;
  }

  public static final class Builder {
    private String keyword;
    private Boolean regexp;
    private Boolean versions;
    private Boolean devel;
    private String version;
    private Boolean failOnNoResult;

    private Builder() {}

    public Builder keyword(String keyword) {
      this.keyword = keyword;
      return this;
    }

    public Builder regexp(boolean regexp) {
      this.regexp = regexp;
      return this;
    }

    public Builder versions(boolean versions) {
      this.versions = versions;
      return this;
    }

    public Builder devel(boolean devel) {
      this.devel = devel;
      return this;
    }

    public Builder version(String version) {
      this.version = version;
      return this;
    }

    public Builder failOnNoResult(boolean failOnNoResult) {
      this.failOnNoResult = failOnNoResult;
      return this;
    }

    public SearchOptions build() {
      return new SearchOptions(this);
    }
  }
}
