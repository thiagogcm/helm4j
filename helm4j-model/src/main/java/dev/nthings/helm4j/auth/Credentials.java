package dev.nthings.helm4j.auth;

import dev.nthings.helm4j.internal.model.ModelSupport;

import org.jspecify.annotations.Nullable;

/** Username/password credentials for a chart repository or OCI registry; blanks become null. */
public record Credentials(@Nullable String username, @Nullable String password) {

  private static final Credentials NONE = new Credentials(null, null);

  public Credentials {
    username = ModelSupport.normalizeBlankToNull(username);
    password = ModelSupport.normalizeBlankToNull(password);
  }

  /** Anonymous access: no username or password. */
  public static Credentials none() {
    return NONE;
  }

  public static Credentials basic(String username, String password) {
    return new Credentials(username, password);
  }
}
