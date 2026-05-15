package dev.nthings.helm4j.client.release;

import java.util.function.Consumer;

import dev.nthings.helm4j.client.internal.NamespaceClient;
import dev.nthings.helm4j.model.ListResult;
import dev.nthings.helm4j.release.GetAllResult;
import dev.nthings.helm4j.release.GetHooksResult;
import dev.nthings.helm4j.release.GetManifestResult;
import dev.nthings.helm4j.release.GetMetadataResult;
import dev.nthings.helm4j.release.GetNotesResult;
import dev.nthings.helm4j.release.GetRequest;
import dev.nthings.helm4j.release.GetValuesResult;
import dev.nthings.helm4j.release.HistoryEntry;
import dev.nthings.helm4j.release.HistoryRequest;
import dev.nthings.helm4j.release.InstallRequest;
import dev.nthings.helm4j.release.ReleaseInfo;
import dev.nthings.helm4j.release.ReleaseListRequest;
import dev.nthings.helm4j.release.ReleaseResult;
import dev.nthings.helm4j.release.RollbackRequest;
import dev.nthings.helm4j.release.RollbackResult;
import dev.nthings.helm4j.release.StatusRequest;
import dev.nthings.helm4j.release.StatusResult;
import dev.nthings.helm4j.release.TestRequest;
import dev.nthings.helm4j.release.TestResult;
import dev.nthings.helm4j.release.UninstallRequest;
import dev.nthings.helm4j.release.UninstallResult;
import dev.nthings.helm4j.release.UpgradeRequest;
import dev.nthings.helm4j.spi.ReleaseGateway;

/**
 * Release namespace for lifecycle and inspection operations.
 *
 * <p>Each operation has two entry points: one that takes a {@link Consumer} configuring a fluent
 * request builder, and an overload that takes a pre-built request for reuse.
 */
public final class ReleaseClient extends NamespaceClient<ReleaseGateway> {

  public ReleaseClient(ReleaseGateway gateway) {
    super(gateway);
  }

  public ReleaseResult install(Consumer<InstallRequest.Builder> spec) {
    var builder = InstallRequest.builder();
    spec.accept(builder);
    return gateway.install(builder.build());
  }

  public ReleaseResult install(InstallRequest request) {
    return gateway.install(request);
  }

  public ReleaseResult upgrade(Consumer<UpgradeRequest.Builder> spec) {
    var builder = UpgradeRequest.builder();
    spec.accept(builder);
    return gateway.upgrade(builder.build());
  }

  public ReleaseResult upgrade(UpgradeRequest request) {
    return gateway.upgrade(request);
  }

  public UninstallResult uninstall(Consumer<UninstallRequest.Builder> spec) {
    var builder = UninstallRequest.builder();
    spec.accept(builder);
    return gateway.uninstall(builder.build());
  }

  public UninstallResult uninstall(UninstallRequest request) {
    return gateway.uninstall(request);
  }

  public StatusResult status(Consumer<StatusRequest.Builder> spec) {
    var builder = StatusRequest.builder();
    spec.accept(builder);
    return gateway.status(builder.build());
  }

  public StatusResult status(StatusRequest request) {
    return gateway.status(request);
  }

  public RollbackResult rollback(Consumer<RollbackRequest.Builder> spec) {
    var builder = RollbackRequest.builder();
    spec.accept(builder);
    return gateway.rollback(builder.build());
  }

  public RollbackResult rollback(RollbackRequest request) {
    return gateway.rollback(request);
  }

  public ListResult<HistoryEntry> history(Consumer<HistoryRequest.Builder> spec) {
    var builder = HistoryRequest.builder();
    spec.accept(builder);
    return gateway.history(builder.build());
  }

  public ListResult<HistoryEntry> history(HistoryRequest request) {
    return gateway.history(request);
  }

  public ListResult<ReleaseInfo> list(Consumer<ReleaseListRequest.Builder> spec) {
    var builder = ReleaseListRequest.builder();
    spec.accept(builder);
    return gateway.list(builder.build());
  }

  public ListResult<ReleaseInfo> list(ReleaseListRequest request) {
    return gateway.list(request);
  }

  public TestResult test(Consumer<TestRequest.Builder> spec) {
    var builder = TestRequest.builder();
    spec.accept(builder);
    return gateway.test(builder.build());
  }

  public TestResult test(TestRequest request) {
    return gateway.test(request);
  }

  public GetAllResult getAll(Consumer<GetRequest.Builder> spec) {
    return gateway.getAll(buildGet(spec));
  }

  public GetAllResult getAll(GetRequest request) {
    return gateway.getAll(request);
  }

  public GetValuesResult getValues(Consumer<GetRequest.Builder> spec) {
    return gateway.getValues(buildGet(spec));
  }

  public GetValuesResult getValues(GetRequest request) {
    return gateway.getValues(request);
  }

  public GetManifestResult getManifest(Consumer<GetRequest.Builder> spec) {
    return gateway.getManifest(buildGet(spec));
  }

  public GetManifestResult getManifest(GetRequest request) {
    return gateway.getManifest(request);
  }

  public GetHooksResult getHooks(Consumer<GetRequest.Builder> spec) {
    return gateway.getHooks(buildGet(spec));
  }

  public GetHooksResult getHooks(GetRequest request) {
    return gateway.getHooks(request);
  }

  public GetNotesResult getNotes(Consumer<GetRequest.Builder> spec) {
    return gateway.getNotes(buildGet(spec));
  }

  public GetNotesResult getNotes(GetRequest request) {
    return gateway.getNotes(request);
  }

  public GetMetadataResult getMetadata(Consumer<GetRequest.Builder> spec) {
    return gateway.getMetadata(buildGet(spec));
  }

  public GetMetadataResult getMetadata(GetRequest request) {
    return gateway.getMetadata(request);
  }

  private static GetRequest buildGet(Consumer<GetRequest.Builder> spec) {
    var builder = GetRequest.builder();
    spec.accept(builder);
    return builder.build();
  }
}
