package dev.nthings.helm4j.release;

import java.util.Objects;
import java.util.function.Consumer;

import dev.nthings.helm4j.internal.api.ClientSupport;
import dev.nthings.helm4j.internal.sdk.HelmGateway;

/** Release namespace for lifecycle operations. */
public final class ReleaseClient {

  private final HelmGateway gateway;

  public ReleaseClient(HelmGateway gateway) {
    this.gateway = Objects.requireNonNull(gateway, "gateway");
  }

  public InstallResult install(Consumer<InstallRequest.Builder> spec) {
    return ClientSupport.buildAndCall(
        InstallRequest::builder, spec, InstallRequest.Builder::build, this::install);
  }

  public InstallResult install(InstallRequest request) {
    Objects.requireNonNull(request, "request");
    return gateway.install(request);
  }

  public UpgradeResult upgrade(Consumer<UpgradeRequest.Builder> spec) {
    return ClientSupport.buildAndCall(
        UpgradeRequest::builder, spec, UpgradeRequest.Builder::build, this::upgrade);
  }

  public UpgradeResult upgrade(UpgradeRequest request) {
    Objects.requireNonNull(request, "request");
    return gateway.upgrade(request);
  }

  public UninstallResult uninstall(Consumer<UninstallRequest.Builder> spec) {
    return ClientSupport.buildAndCall(
        UninstallRequest::builder, spec, UninstallRequest.Builder::build, this::uninstall);
  }

  public UninstallResult uninstall(UninstallRequest request) {
    Objects.requireNonNull(request, "request");
    return gateway.uninstall(request);
  }

  public StatusResult status(Consumer<StatusRequest.Builder> spec) {
    return ClientSupport.buildAndCall(
        StatusRequest::builder, spec, StatusRequest.Builder::build, this::status);
  }

  public StatusResult status(StatusRequest request) {
    Objects.requireNonNull(request, "request");
    return gateway.status(request);
  }

  public RollbackResult rollback(Consumer<RollbackRequest.Builder> spec) {
    return ClientSupport.buildAndCall(
        RollbackRequest::builder, spec, RollbackRequest.Builder::build, this::rollback);
  }

  public RollbackResult rollback(RollbackRequest request) {
    Objects.requireNonNull(request, "request");
    return gateway.rollback(request);
  }

  public HistoryResult history(Consumer<HistoryRequest.Builder> spec) {
    return ClientSupport.buildAndCall(
        HistoryRequest::builder, spec, HistoryRequest.Builder::build, this::history);
  }

  public HistoryResult history(HistoryRequest request) {
    Objects.requireNonNull(request, "request");
    return gateway.history(request);
  }

  public ReleaseListResult list() {
    return list(ReleaseListRequest.builder().build());
  }

  public ReleaseListResult list(Consumer<ReleaseListRequest.Builder> spec) {
    return ClientSupport.buildAndCall(
        ReleaseListRequest::builder, spec, ReleaseListRequest.Builder::build, this::list);
  }

  public ReleaseListResult list(ReleaseListRequest request) {
    Objects.requireNonNull(request, "request");
    return gateway.list(request);
  }

  public TestResult test(Consumer<TestRequest.Builder> spec) {
    return ClientSupport.buildAndCall(
        TestRequest::builder, spec, TestRequest.Builder::build, this::test);
  }

  public TestResult test(TestRequest request) {
    Objects.requireNonNull(request, "request");
    return gateway.test(request);
  }

  public GetAllResult getAll(Consumer<GetRequest.Builder> spec) {
    return ClientSupport.buildAndCall(
        GetRequest::builder, spec, GetRequest.Builder::build, this::getAll);
  }

  public GetAllResult getAll(GetRequest request) {
    Objects.requireNonNull(request, "request");
    return gateway.getAll(request);
  }

  public GetValuesResult getValues(Consumer<GetRequest.Builder> spec) {
    return ClientSupport.buildAndCall(
        GetRequest::builder, spec, GetRequest.Builder::build, this::getValues);
  }

  public GetValuesResult getValues(GetRequest request) {
    Objects.requireNonNull(request, "request");
    return gateway.getValues(request);
  }

  public GetManifestResult getManifest(Consumer<GetRequest.Builder> spec) {
    return ClientSupport.buildAndCall(
        GetRequest::builder, spec, GetRequest.Builder::build, this::getManifest);
  }

  public GetManifestResult getManifest(GetRequest request) {
    Objects.requireNonNull(request, "request");
    return gateway.getManifest(request);
  }

  public GetHooksResult getHooks(Consumer<GetRequest.Builder> spec) {
    return ClientSupport.buildAndCall(
        GetRequest::builder, spec, GetRequest.Builder::build, this::getHooks);
  }

  public GetHooksResult getHooks(GetRequest request) {
    Objects.requireNonNull(request, "request");
    return gateway.getHooks(request);
  }

  public GetNotesResult getNotes(Consumer<GetRequest.Builder> spec) {
    return ClientSupport.buildAndCall(
        GetRequest::builder, spec, GetRequest.Builder::build, this::getNotes);
  }

  public GetNotesResult getNotes(GetRequest request) {
    Objects.requireNonNull(request, "request");
    return gateway.getNotes(request);
  }

  public GetMetadataResult getMetadata(Consumer<GetRequest.Builder> spec) {
    return ClientSupport.buildAndCall(
        GetRequest::builder, spec, GetRequest.Builder::build, this::getMetadata);
  }

  public GetMetadataResult getMetadata(GetRequest request) {
    Objects.requireNonNull(request, "request");
    return gateway.getMetadata(request);
  }
}
