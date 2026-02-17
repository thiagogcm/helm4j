package dev.nthings.helm4j.client;

import java.util.List;
import java.util.Objects;

import dev.nthings.helm4j.bindings.NativeHelmBindings;
import dev.nthings.helm4j.bindings.NativePayloadCodec;
import dev.nthings.helm4j.bindings.NativePayloadMapper;
import dev.nthings.helm4j.model.RepoAddResult;
import dev.nthings.helm4j.model.RepoListResult;
import dev.nthings.helm4j.model.RepoRemoveResult;
import dev.nthings.helm4j.model.RepoUpdateResult;
import dev.nthings.helm4j.options.RepoAddOptions;
import dev.nthings.helm4j.options.RepoListOptions;
import dev.nthings.helm4j.options.RepoRemoveOptions;
import dev.nthings.helm4j.options.RepoUpdateOptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Consumer-facing Java API for {@code helm repo} operations. */
public final class HelmRepoClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(HelmRepoClient.class);

  private final NativeHelmBindings bindings;
  private final NativePayloadCodec payloadCodec;

  HelmRepoClient(NativeHelmBindings bindings, NativePayloadCodec payloadCodec) {
    this.bindings = Objects.requireNonNull(bindings, "bindings");
    this.payloadCodec = Objects.requireNonNull(payloadCodec, "payloadCodec");
  }

  /** Add a repository (equivalent to {@code helm repo add}). */
  public RepoAddResult add(String name, String url) {
    return add(RepoAddOptions.builder().name(name).url(url).build());
  }

  /** Add a repository (equivalent to {@code helm repo add}). */
  public RepoAddResult add(RepoAddOptions options) {
    Objects.requireNonNull(options, "options");
    LOGGER.debug("Running helm repo add for name='{}'", options.name());

    var payload = NativePayloadMapper.toNativeRepoAddOptions(options);
    var optionsJson = payloadCodec.toJson(payload);
    var responseJson = bindings.repoAdd(optionsJson);
    var nativeResponse = payloadCodec.decodeRepoAddResponse(responseJson);
    var response = NativePayloadMapper.toRepoAddResult(nativeResponse);

    LOGGER.debug("Helm repo add completed for name='{}'", response.name());
    return response;
  }

  /** Update all repositories (equivalent to {@code helm repo update}). */
  public RepoUpdateResult updateAll() {
    return update(RepoUpdateOptions.defaults());
  }

  /** Update repositories (equivalent to {@code helm repo update}). */
  public RepoUpdateResult update(RepoUpdateOptions options) {
    Objects.requireNonNull(options, "options");
    LOGGER.debug("Running helm repo update for names='{}'", options.names());

    var payload = NativePayloadMapper.toNativeRepoUpdateOptions(options);
    var optionsJson = payloadCodec.toJson(payload);
    var responseJson = bindings.repoUpdate(optionsJson);
    var nativeResponse = payloadCodec.decodeRepoUpdateResponse(responseJson);
    var response = NativePayloadMapper.toRepoUpdateResult(nativeResponse);

    LOGGER.debug("Helm repo update completed with {} result(s)", response.size());
    return response;
  }

  /** List repositories (equivalent to {@code helm repo list}). */
  public RepoListResult list() {
    return list(RepoListOptions.defaults());
  }

  /** List repositories (equivalent to {@code helm repo list}). */
  public RepoListResult list(RepoListOptions options) {
    Objects.requireNonNull(options, "options");
    LOGGER.debug("Running helm repo list");

    var payload = NativePayloadMapper.toNativeRepoListOptions(options);
    var optionsJson = payloadCodec.toJson(payload);
    var responseJson = bindings.repoList(optionsJson);
    var nativeResponse = payloadCodec.decodeRepoListResponse(responseJson);
    var response = NativePayloadMapper.toRepoListResult(nativeResponse);

    LOGGER.debug("Helm repo list completed with {} entries", response.size());
    return response;
  }

  /** Remove repositories (equivalent to {@code helm repo remove}). */
  public RepoRemoveResult remove(List<String> names) {
    Objects.requireNonNull(names, "names");
    return remove(RepoRemoveOptions.builder().names(names).build());
  }

  /** Remove repositories (equivalent to {@code helm repo remove}). */
  public RepoRemoveResult remove(RepoRemoveOptions options) {
    Objects.requireNonNull(options, "options");
    LOGGER.debug("Running helm repo remove for names='{}'", options.names());

    var payload = NativePayloadMapper.toNativeRepoRemoveOptions(options);
    var optionsJson = payloadCodec.toJson(payload);
    var responseJson = bindings.repoRemove(optionsJson);
    var nativeResponse = payloadCodec.decodeRepoRemoveResponse(responseJson);
    var response = NativePayloadMapper.toRepoRemoveResult(nativeResponse);

    LOGGER.debug("Helm repo remove completed with {} removed entries", response.size());
    return response;
  }
}
