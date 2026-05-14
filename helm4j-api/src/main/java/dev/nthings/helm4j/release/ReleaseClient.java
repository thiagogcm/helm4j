package dev.nthings.helm4j.release;

import dev.nthings.helm4j.internal.api.NamespaceClient;
import dev.nthings.helm4j.internal.gateway.ReleaseGateway;
import dev.nthings.helm4j.model.ListResult;

/**
 * Release namespace for lifecycle and inspection operations.
 *
 * <p>Each operation has two entry points: a no-argument method that returns a runnable, fluent
 * request builder (call {@code execute()} on it), and an overload that takes a pre-built request
 * for reuse.
 */
public final class ReleaseClient extends NamespaceClient<ReleaseGateway> {

  public ReleaseClient(ReleaseGateway gateway) {
    super(gateway);
  }

  /** Begins a fluent install; call {@code execute()} to run it. */
  public InstallRequest.Builder install() {
    return InstallRequest.builder(gateway);
  }

  public ReleaseResult install(InstallRequest request) {
    return gateway.install(request);
  }

  /** Begins a fluent upgrade; call {@code execute()} to run it. */
  public UpgradeRequest.Builder upgrade() {
    return UpgradeRequest.builder(gateway);
  }

  public ReleaseResult upgrade(UpgradeRequest request) {
    return gateway.upgrade(request);
  }

  /** Begins a fluent uninstall; call {@code execute()} to run it. */
  public UninstallRequest.Builder uninstall() {
    return UninstallRequest.builder(gateway);
  }

  public UninstallResult uninstall(UninstallRequest request) {
    return gateway.uninstall(request);
  }

  /** Begins a fluent status query; call {@code execute()} to run it. */
  public StatusRequest.Builder status() {
    return StatusRequest.builder(gateway);
  }

  public StatusResult status(StatusRequest request) {
    return gateway.status(request);
  }

  /** Begins a fluent rollback; call {@code execute()} to run it. */
  public RollbackRequest.Builder rollback() {
    return RollbackRequest.builder(gateway);
  }

  public RollbackResult rollback(RollbackRequest request) {
    return gateway.rollback(request);
  }

  /** Begins a fluent history query; call {@code execute()} to run it. */
  public HistoryRequest.Builder history() {
    return HistoryRequest.builder(gateway);
  }

  public ListResult<HistoryEntry> history(HistoryRequest request) {
    return gateway.history(request);
  }

  /** Begins a fluent release listing; call {@code execute()} to run it. */
  public ReleaseListRequest.Builder list() {
    return ReleaseListRequest.builder(gateway);
  }

  public ListResult<ReleaseInfo> list(ReleaseListRequest request) {
    return gateway.list(request);
  }

  /** Begins a fluent test run; call {@code execute()} to run it. */
  public TestRequest.Builder test() {
    return TestRequest.builder(gateway);
  }

  public TestResult test(TestRequest request) {
    return gateway.test(request);
  }

  /**
   * Begins a fluent {@code get} query. The variant is chosen by the terminal method on the builder:
   * {@code all()}, {@code values()}, {@code manifest()}, {@code hooks()}, {@code notes()} or {@code
   * metadata()}.
   */
  public GetRequest.Builder get() {
    return GetRequest.builder(gateway);
  }

  public GetAllResult getAll(GetRequest request) {
    return gateway.getAll(request);
  }

  public GetValuesResult getValues(GetRequest request) {
    return gateway.getValues(request);
  }

  public GetManifestResult getManifest(GetRequest request) {
    return gateway.getManifest(request);
  }

  public GetHooksResult getHooks(GetRequest request) {
    return gateway.getHooks(request);
  }

  public GetNotesResult getNotes(GetRequest request) {
    return gateway.getNotes(request);
  }

  public GetMetadataResult getMetadata(GetRequest request) {
    return gateway.getMetadata(request);
  }
}
