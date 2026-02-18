package dev.nthings.helm4j.release;

import java.util.Objects;
import java.util.function.Consumer;

import dev.nthings.helm4j.internal.sdk.HelmGateway;

/** Release namespace for lifecycle operations. */
public final class ReleaseClient {

  private final HelmGateway gateway;

  public ReleaseClient(HelmGateway gateway) {
    this.gateway = Objects.requireNonNull(gateway, "gateway");
  }

  public InstallResult install(Consumer<InstallRequest.Builder> spec) {
    Objects.requireNonNull(spec, "spec");
    var builder = InstallRequest.builder();
    spec.accept(builder);
    return install(builder.build());
  }

  public InstallResult install(InstallRequest request) {
    Objects.requireNonNull(request, "request");
    return gateway.install(request);
  }

  public UpgradeResult upgrade(Consumer<UpgradeRequest.Builder> spec) {
    Objects.requireNonNull(spec, "spec");
    var builder = UpgradeRequest.builder();
    spec.accept(builder);
    return upgrade(builder.build());
  }

  public UpgradeResult upgrade(UpgradeRequest request) {
    Objects.requireNonNull(request, "request");
    return gateway.upgrade(request);
  }

  public UninstallResult uninstall(String releaseName) {
    return uninstall(UninstallRequest.builder().releaseName(releaseName).build());
  }

  public UninstallResult uninstall(Consumer<UninstallRequest.Builder> spec) {
    Objects.requireNonNull(spec, "spec");
    var builder = UninstallRequest.builder();
    spec.accept(builder);
    return uninstall(builder.build());
  }

  public UninstallResult uninstall(UninstallRequest request) {
    Objects.requireNonNull(request, "request");
    return gateway.uninstall(request);
  }

  public StatusResult status(String releaseName) {
    return status(StatusRequest.builder().releaseName(releaseName).build());
  }

  public StatusResult status(Consumer<StatusRequest.Builder> spec) {
    Objects.requireNonNull(spec, "spec");
    var builder = StatusRequest.builder();
    spec.accept(builder);
    return status(builder.build());
  }

  public StatusResult status(StatusRequest request) {
    Objects.requireNonNull(request, "request");
    return gateway.status(request);
  }

  public RollbackResult rollback(Consumer<RollbackRequest.Builder> spec) {
    Objects.requireNonNull(spec, "spec");
    var builder = RollbackRequest.builder();
    spec.accept(builder);
    return rollback(builder.build());
  }

  public RollbackResult rollback(RollbackRequest request) {
    Objects.requireNonNull(request, "request");
    return gateway.rollback(request);
  }

  public HistoryResult history(String releaseName) {
    return history(HistoryRequest.builder().releaseName(releaseName).build());
  }

  public HistoryResult history(Consumer<HistoryRequest.Builder> spec) {
    Objects.requireNonNull(spec, "spec");
    var builder = HistoryRequest.builder();
    spec.accept(builder);
    return history(builder.build());
  }

  public HistoryResult history(HistoryRequest request) {
    Objects.requireNonNull(request, "request");
    return gateway.history(request);
  }

  public ReleaseListResult list() {
    return list(ReleaseListRequest.builder().build());
  }

  public ReleaseListResult list(Consumer<ReleaseListRequest.Builder> spec) {
    Objects.requireNonNull(spec, "spec");
    var builder = ReleaseListRequest.builder();
    spec.accept(builder);
    return list(builder.build());
  }

  public ReleaseListResult list(ReleaseListRequest request) {
    Objects.requireNonNull(request, "request");
    return gateway.list(request);
  }

  public TestResult test(String releaseName) {
    return test(TestRequest.builder().releaseName(releaseName).build());
  }

  public TestResult test(Consumer<TestRequest.Builder> spec) {
    Objects.requireNonNull(spec, "spec");
    var builder = TestRequest.builder();
    spec.accept(builder);
    return test(builder.build());
  }

  public TestResult test(TestRequest request) {
    Objects.requireNonNull(request, "request");
    return gateway.test(request);
  }

  public GetAllResult getAll(String releaseName) {
    return getAll(GetRequest.builder().releaseName(releaseName).build());
  }

  public GetAllResult getAll(Consumer<GetRequest.Builder> spec) {
    Objects.requireNonNull(spec, "spec");
    var builder = GetRequest.builder();
    spec.accept(builder);
    return getAll(builder.build());
  }

  public GetAllResult getAll(GetRequest request) {
    Objects.requireNonNull(request, "request");
    return gateway.getAll(request);
  }

  public GetValuesResult getValues(String releaseName) {
    return getValues(GetRequest.builder().releaseName(releaseName).build());
  }

  public GetValuesResult getValues(Consumer<GetRequest.Builder> spec) {
    Objects.requireNonNull(spec, "spec");
    var builder = GetRequest.builder();
    spec.accept(builder);
    return getValues(builder.build());
  }

  public GetValuesResult getValues(GetRequest request) {
    Objects.requireNonNull(request, "request");
    return gateway.getValues(request);
  }

  public GetManifestResult getManifest(String releaseName) {
    return getManifest(GetRequest.builder().releaseName(releaseName).build());
  }

  public GetManifestResult getManifest(Consumer<GetRequest.Builder> spec) {
    Objects.requireNonNull(spec, "spec");
    var builder = GetRequest.builder();
    spec.accept(builder);
    return getManifest(builder.build());
  }

  public GetManifestResult getManifest(GetRequest request) {
    Objects.requireNonNull(request, "request");
    return gateway.getManifest(request);
  }

  public GetHooksResult getHooks(String releaseName) {
    return getHooks(GetRequest.builder().releaseName(releaseName).build());
  }

  public GetHooksResult getHooks(Consumer<GetRequest.Builder> spec) {
    Objects.requireNonNull(spec, "spec");
    var builder = GetRequest.builder();
    spec.accept(builder);
    return getHooks(builder.build());
  }

  public GetHooksResult getHooks(GetRequest request) {
    Objects.requireNonNull(request, "request");
    return gateway.getHooks(request);
  }

  public GetNotesResult getNotes(String releaseName) {
    return getNotes(GetRequest.builder().releaseName(releaseName).build());
  }

  public GetNotesResult getNotes(Consumer<GetRequest.Builder> spec) {
    Objects.requireNonNull(spec, "spec");
    var builder = GetRequest.builder();
    spec.accept(builder);
    return getNotes(builder.build());
  }

  public GetNotesResult getNotes(GetRequest request) {
    Objects.requireNonNull(request, "request");
    return gateway.getNotes(request);
  }

  public GetMetadataResult getMetadata(String releaseName) {
    return getMetadata(GetRequest.builder().releaseName(releaseName).build());
  }

  public GetMetadataResult getMetadata(Consumer<GetRequest.Builder> spec) {
    Objects.requireNonNull(spec, "spec");
    var builder = GetRequest.builder();
    spec.accept(builder);
    return getMetadata(builder.build());
  }

  public GetMetadataResult getMetadata(GetRequest request) {
    Objects.requireNonNull(request, "request");
    return gateway.getMetadata(request);
  }
}
