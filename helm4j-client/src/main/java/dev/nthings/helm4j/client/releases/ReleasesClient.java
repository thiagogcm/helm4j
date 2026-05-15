package dev.nthings.helm4j.client.releases;

import java.util.function.Consumer;

import dev.nthings.helm4j.client.internal.NamespaceClient;
import dev.nthings.helm4j.model.ListResult;
import dev.nthings.helm4j.release.GetAllResult;
import dev.nthings.helm4j.release.GetHooksResult;
import dev.nthings.helm4j.release.GetManifestResult;
import dev.nthings.helm4j.release.GetMetadataResult;
import dev.nthings.helm4j.release.GetNotesResult;
import dev.nthings.helm4j.release.GetRelease;
import dev.nthings.helm4j.release.GetValuesResult;
import dev.nthings.helm4j.release.HistoryEntry;
import dev.nthings.helm4j.release.InstallRelease;
import dev.nthings.helm4j.release.ListReleases;
import dev.nthings.helm4j.release.Release;
import dev.nthings.helm4j.release.ReleaseHistory;
import dev.nthings.helm4j.release.RollbackRelease;
import dev.nthings.helm4j.release.RollbackReport;
import dev.nthings.helm4j.release.StatusRelease;
import dev.nthings.helm4j.release.TestRelease;
import dev.nthings.helm4j.release.TestResult;
import dev.nthings.helm4j.release.UninstallRelease;
import dev.nthings.helm4j.release.UninstallReport;
import dev.nthings.helm4j.release.UpgradeRelease;
import dev.nthings.helm4j.spi.ReleaseGateway;

/**
 * Release namespace for lifecycle and inspection operations.
 *
 * <p>Each operation has two entry points: one taking a {@link Consumer} that configures a fluent
 * request builder, and an overload taking a pre-built request for reuse. Mutations throw {@code
 * HelmCommandException} on Helm failure; inspect {@link Release#status()} for pending states.
 */
public final class ReleasesClient extends NamespaceClient<ReleaseGateway> {

  public ReleasesClient(ReleaseGateway gateway) {
    super(gateway);
  }

  public Release install(Consumer<InstallRelease.Builder> spec) {
    return gateway.install(configured(InstallRelease::builder, spec).build());
  }

  public Release install(InstallRelease request) {
    return gateway.install(request);
  }

  public Release upgrade(Consumer<UpgradeRelease.Builder> spec) {
    return gateway.upgrade(configured(UpgradeRelease::builder, spec).build());
  }

  public Release upgrade(UpgradeRelease request) {
    return gateway.upgrade(request);
  }

  public UninstallReport uninstall(Consumer<UninstallRelease.Builder> spec) {
    return gateway.uninstall(configured(UninstallRelease::builder, spec).build());
  }

  public UninstallReport uninstall(UninstallRelease request) {
    return gateway.uninstall(request);
  }

  public Release status(Consumer<StatusRelease.Builder> spec) {
    return gateway.status(configured(StatusRelease::builder, spec).build());
  }

  public Release status(StatusRelease request) {
    return gateway.status(request);
  }

  public RollbackReport rollback(Consumer<RollbackRelease.Builder> spec) {
    return gateway.rollback(configured(RollbackRelease::builder, spec).build());
  }

  public RollbackReport rollback(RollbackRelease request) {
    return gateway.rollback(request);
  }

  public ListResult<HistoryEntry> history(Consumer<ReleaseHistory.Builder> spec) {
    return gateway.history(configured(ReleaseHistory::builder, spec).build());
  }

  public ListResult<HistoryEntry> history(ReleaseHistory request) {
    return gateway.history(request);
  }

  public ListResult<Release> list(Consumer<ListReleases.Builder> spec) {
    return gateway.list(configured(ListReleases::builder, spec).build());
  }

  public ListResult<Release> list(ListReleases request) {
    return gateway.list(request);
  }

  public TestResult test(Consumer<TestRelease.Builder> spec) {
    return gateway.test(configured(TestRelease::builder, spec).build());
  }

  public TestResult test(TestRelease request) {
    return gateway.test(request);
  }

  public GetAllResult getAll(Consumer<GetRelease.Builder> spec) {
    return gateway.getAll(configured(GetRelease::builder, spec).build());
  }

  public GetAllResult getAll(GetRelease request) {
    return gateway.getAll(request);
  }

  public GetValuesResult getValues(Consumer<GetRelease.Builder> spec) {
    return gateway.getValues(configured(GetRelease::builder, spec).build());
  }

  public GetValuesResult getValues(GetRelease request) {
    return gateway.getValues(request);
  }

  public GetManifestResult getManifest(Consumer<GetRelease.Builder> spec) {
    return gateway.getManifest(configured(GetRelease::builder, spec).build());
  }

  public GetManifestResult getManifest(GetRelease request) {
    return gateway.getManifest(request);
  }

  public GetHooksResult getHooks(Consumer<GetRelease.Builder> spec) {
    return gateway.getHooks(configured(GetRelease::builder, spec).build());
  }

  public GetHooksResult getHooks(GetRelease request) {
    return gateway.getHooks(request);
  }

  public GetNotesResult getNotes(Consumer<GetRelease.Builder> spec) {
    return gateway.getNotes(configured(GetRelease::builder, spec).build());
  }

  public GetNotesResult getNotes(GetRelease request) {
    return gateway.getNotes(request);
  }

  public GetMetadataResult getMetadata(Consumer<GetRelease.Builder> spec) {
    return gateway.getMetadata(configured(GetRelease::builder, spec).build());
  }

  public GetMetadataResult getMetadata(GetRelease request) {
    return gateway.getMetadata(request);
  }
}
