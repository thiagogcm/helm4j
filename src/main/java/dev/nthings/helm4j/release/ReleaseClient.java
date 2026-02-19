package dev.nthings.helm4j.release;

import java.util.function.Consumer;

import dev.nthings.helm4j.internal.api.NamespaceClient;
import dev.nthings.helm4j.internal.sdk.HelmGateway;
import dev.nthings.helm4j.model.ListResult;

/** Release namespace for lifecycle operations. */
public final class ReleaseClient extends NamespaceClient {

  public ReleaseClient(HelmGateway gateway) {
    super(gateway);
  }

  public ReleaseOutcome install(Consumer<InstallRequest.Builder> spec) {
    return buildAndInvoke(
        InstallRequest::builder, spec, InstallRequest.Builder::build, this::install);
  }

  public ReleaseOutcome install(InstallRequest request) {
    return invoke(request, gateway::install);
  }

  public ReleaseOutcome upgrade(Consumer<UpgradeRequest.Builder> spec) {
    return buildAndInvoke(
        UpgradeRequest::builder, spec, UpgradeRequest.Builder::build, this::upgrade);
  }

  public ReleaseOutcome upgrade(UpgradeRequest request) {
    return invoke(request, gateway::upgrade);
  }

  public ReleaseOutcome uninstall(Consumer<UninstallRequest.Builder> spec) {
    return buildAndInvoke(
        UninstallRequest::builder, spec, UninstallRequest.Builder::build, this::uninstall);
  }

  public ReleaseOutcome uninstall(UninstallRequest request) {
    return invoke(request, gateway::uninstall);
  }

  public StatusResult status(Consumer<StatusRequest.Builder> spec) {
    return buildAndInvoke(StatusRequest::builder, spec, StatusRequest.Builder::build, this::status);
  }

  public StatusResult status(StatusRequest request) {
    return invoke(request, gateway::status);
  }

  public ReleaseOutcome rollback(Consumer<RollbackRequest.Builder> spec) {
    return buildAndInvoke(
        RollbackRequest::builder, spec, RollbackRequest.Builder::build, this::rollback);
  }

  public ReleaseOutcome rollback(RollbackRequest request) {
    return invoke(request, gateway::rollback);
  }

  public ListResult<HistoryEntry> history(Consumer<HistoryRequest.Builder> spec) {
    return buildAndInvoke(
        HistoryRequest::builder, spec, HistoryRequest.Builder::build, this::history);
  }

  public ListResult<HistoryEntry> history(HistoryRequest request) {
    return invoke(request, gateway::history);
  }

  public ListResult<ReleaseInfo> list() {
    return list(ReleaseListRequest.builder().build());
  }

  public ListResult<ReleaseInfo> list(Consumer<ReleaseListRequest.Builder> spec) {
    return buildAndInvoke(
        ReleaseListRequest::builder, spec, ReleaseListRequest.Builder::build, this::list);
  }

  public ListResult<ReleaseInfo> list(ReleaseListRequest request) {
    return invoke(request, gateway::list);
  }

  public TestResult test(Consumer<TestRequest.Builder> spec) {
    return buildAndInvoke(TestRequest::builder, spec, TestRequest.Builder::build, this::test);
  }

  public TestResult test(TestRequest request) {
    return invoke(request, gateway::test);
  }

  public GetAllResult getAll(Consumer<GetRequest.Builder> spec) {
    return buildAndInvoke(GetRequest::builder, spec, GetRequest.Builder::build, this::getAll);
  }

  public GetAllResult getAll(GetRequest request) {
    return invoke(request, gateway::getAll);
  }

  public GetValuesResult getValues(Consumer<GetRequest.Builder> spec) {
    return buildAndInvoke(GetRequest::builder, spec, GetRequest.Builder::build, this::getValues);
  }

  public GetValuesResult getValues(GetRequest request) {
    return invoke(request, gateway::getValues);
  }

  public GetManifestResult getManifest(Consumer<GetRequest.Builder> spec) {
    return buildAndInvoke(GetRequest::builder, spec, GetRequest.Builder::build, this::getManifest);
  }

  public GetManifestResult getManifest(GetRequest request) {
    return invoke(request, gateway::getManifest);
  }

  public GetHooksResult getHooks(Consumer<GetRequest.Builder> spec) {
    return buildAndInvoke(GetRequest::builder, spec, GetRequest.Builder::build, this::getHooks);
  }

  public GetHooksResult getHooks(GetRequest request) {
    return invoke(request, gateway::getHooks);
  }

  public GetNotesResult getNotes(Consumer<GetRequest.Builder> spec) {
    return buildAndInvoke(GetRequest::builder, spec, GetRequest.Builder::build, this::getNotes);
  }

  public GetNotesResult getNotes(GetRequest request) {
    return invoke(request, gateway::getNotes);
  }

  public GetMetadataResult getMetadata(Consumer<GetRequest.Builder> spec) {
    return buildAndInvoke(GetRequest::builder, spec, GetRequest.Builder::build, this::getMetadata);
  }

  public GetMetadataResult getMetadata(GetRequest request) {
    return invoke(request, gateway::getMetadata);
  }
}
