package dev.nthings.helm4j.runtime.ffm.internal;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import dev.nthings.helm4j.errors.HelmConfigurationException;
import dev.nthings.helm4j.model.ListResult;
import dev.nthings.helm4j.release.GetAllResult;
import dev.nthings.helm4j.release.GetHooksResult;
import dev.nthings.helm4j.release.GetManifestResult;
import dev.nthings.helm4j.release.GetMetadataResult;
import dev.nthings.helm4j.release.GetMode;
import dev.nthings.helm4j.release.GetNotesResult;
import dev.nthings.helm4j.release.GetRelease;
import dev.nthings.helm4j.release.GetValuesResult;
import dev.nthings.helm4j.release.HistoryEntry;
import dev.nthings.helm4j.release.InstallRelease;
import dev.nthings.helm4j.release.ListReleases;
import dev.nthings.helm4j.release.Release;
import dev.nthings.helm4j.release.ReleaseHistory;
import dev.nthings.helm4j.release.ReleaseStatus;
import dev.nthings.helm4j.release.RollbackRelease;
import dev.nthings.helm4j.release.RollbackReport;
import dev.nthings.helm4j.release.StatusRelease;
import dev.nthings.helm4j.release.TestHookResult;
import dev.nthings.helm4j.release.TestRelease;
import dev.nthings.helm4j.release.TestResult;
import dev.nthings.helm4j.release.UninstallRelease;
import dev.nthings.helm4j.release.UninstallReport;
import dev.nthings.helm4j.release.UpgradeRelease;
import dev.nthings.helm4j.spi.ReleaseGateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.jackson.databind.JsonNode;

import static dev.nthings.helm4j.runtime.ffm.internal.NativeGatewaySupport.listOrEmpty;
import static dev.nthings.helm4j.runtime.ffm.internal.NativeGatewaySupport.mapHooks;
import static dev.nthings.helm4j.runtime.ffm.internal.NativeGatewaySupport.mapOrEmpty;
import static dev.nthings.helm4j.runtime.ffm.internal.NativeGatewaySupport.mapReleasePayload;
import static dev.nthings.helm4j.runtime.ffm.internal.NativeGatewaySupport.parseTimestamp;
import static dev.nthings.helm4j.runtime.ffm.internal.NativeGatewaySupport.requireResponse;
import static dev.nthings.helm4j.runtime.ffm.internal.NativeGatewaySupport.utf8;

/** Release lifecycle and inspection operations backed by the JSON bridge. */
final class NativeReleaseGateway implements ReleaseGateway {

  private static final Logger log = LoggerFactory.getLogger(NativeReleaseGateway.class);

  private final NativeGatewaySupport support;

  NativeReleaseGateway(NativeGatewaySupport support) {
    this.support = Objects.requireNonNull(support, "support");
  }

  @Override
  public Release install(InstallRelease request) {
    Objects.requireNonNull(request, "request");
    if (request.chart() == null) {
      throw new HelmConfigurationException("Install requires chart reference");
    }

    log.debug(
        "Installing release: name={}, chart={}",
        request.releaseName(),
        request.chart().asReference());
    var root =
        support.invokeRootOrThrow(
            "install",
            bridge ->
                bridge.install(
                    utf8(request.releaseName()),
                    utf8(request.chart().asReference()),
                    support.toJsonBytes(NativeOptions.install(request), "install")));

    var response =
        requireResponse(support.convert(root, ReleasePayload.class, "install"), "install", "data");
    return mapReleasePayload(requireResponse(response.release(), "install", "release"), "install");
  }

  @Override
  public Release upgrade(UpgradeRelease request) {
    Objects.requireNonNull(request, "request");
    if (request.chart() == null) {
      throw new HelmConfigurationException("Upgrade requires chart reference");
    }

    log.debug(
        "Upgrading release: name={}, chart={}",
        request.releaseName(),
        request.chart().asReference());
    var root =
        support.invokeRootOrThrow(
            "upgrade",
            bridge ->
                bridge.upgrade(
                    utf8(request.releaseName()),
                    utf8(request.chart().asReference()),
                    support.toJsonBytes(NativeOptions.upgrade(request), "upgrade")));

    var response =
        requireResponse(support.convert(root, ReleasePayload.class, "upgrade"), "upgrade", "data");
    return mapReleasePayload(requireResponse(response.release(), "upgrade", "release"), "upgrade");
  }

  @Override
  public UninstallReport uninstall(UninstallRelease request) {
    Objects.requireNonNull(request, "request");

    log.debug("Uninstalling release: name={}", request.releaseName());
    var root =
        support.invokeRootOrThrow(
            "uninstall",
            bridge ->
                bridge.uninstall(
                    utf8(request.releaseName()),
                    support.toJsonBytes(NativeOptions.uninstall(request), "uninstall")));

    var response =
        requireResponse(
            support.convert(root, UninstallPayload.class, "uninstall"), "uninstall", "data");
    var release =
        response.release() != null ? mapReleasePayload(response.release(), "uninstall") : null;
    return new UninstallReport(release, response.info());
  }

  @Override
  public Release status(StatusRelease request) {
    Objects.requireNonNull(request, "request");

    log.debug("Getting status: name={}", request.releaseName());
    var root =
        support.invokeRootOrThrow(
            "status",
            bridge ->
                bridge.status(
                    utf8(request.releaseName()),
                    support.toJsonBytes(NativeOptions.status(request), "status")));

    var response =
        requireResponse(support.convert(root, ReleasePayload.class, "status"), "status", "data");
    return mapReleasePayload(requireResponse(response.release(), "status", "release"), "status");
  }

  @Override
  public RollbackReport rollback(RollbackRelease request) {
    Objects.requireNonNull(request, "request");

    log.debug("Rolling back release: name={}", request.releaseName());
    var root =
        support.invokeRootOrThrow(
            "rollback",
            bridge ->
                bridge.rollback(
                    utf8(request.releaseName()),
                    support.toJsonBytes(NativeOptions.rollback(request), "rollback")));

    var response =
        requireResponse(
            support.convert(root, RollbackPayload.class, "rollback"), "rollback", "data");
    return new RollbackReport(response.releaseName(), response.revision());
  }

  @Override
  public ListResult<HistoryEntry> history(ReleaseHistory request) {
    Objects.requireNonNull(request, "request");

    log.debug("Getting history: name={}", request.releaseName());
    var root =
        support.invokeRootOrThrow(
            "history",
            bridge ->
                bridge.history(
                    utf8(request.releaseName()),
                    support.toJsonBytes(NativeOptions.history(request), "history")));

    var response = support.convert(root, HistoryPayload.class, "history");
    var entries =
        listOrEmpty(response == null ? null : response.entries()).stream()
            .map(
                e ->
                    new HistoryEntry(
                        e.revision(),
                        parseTimestamp(e.updated(), "history", "updated"),
                        ReleaseStatus.fromWireValue(e.status()),
                        e.chart(),
                        e.chartVersion(),
                        e.appVersion(),
                        e.description()))
            .toList();
    return ListResult.of(entries);
  }

  @Override
  public ListResult<Release> list(ListReleases request) {
    Objects.requireNonNull(request, "request");

    log.debug(
        "Listing releases: namespace={}, allNamespaces={}",
        request.namespace(),
        request.allNamespaces());
    var root =
        support.invokeRootOrThrow(
            "list",
            bridge -> bridge.list(support.toJsonBytes(NativeOptions.list(request), "list")));

    var response = support.convert(root, ListPayload.class, "list");
    var releases =
        listOrEmpty(response == null ? null : response.releases()).stream()
            .map(release -> mapReleasePayload(release, "list"))
            .toList();
    return ListResult.of(releases);
  }

  @Override
  public TestResult test(TestRelease request) {
    Objects.requireNonNull(request, "request");
    if (request.releaseName() == null) {
      throw new HelmConfigurationException("Test requires release name");
    }

    log.debug("Testing release: name={}", request.releaseName());
    var root =
        support.invokeRootOrThrow(
            "test",
            bridge ->
                bridge.test(
                    utf8(request.releaseName()),
                    support.toJsonBytes(NativeOptions.test(request), "test")));

    var response =
        requireResponse(support.convert(root, TestPayload.class, "test"), "test", "data");
    var release = requireResponse(response.release(), "test", "release");
    var results =
        listOrEmpty(response.results()).stream()
            .map(r -> new TestHookResult(r.name(), r.status()))
            .toList();
    return new TestResult(mapReleasePayload(release, "test"), results);
  }

  @Override
  public GetAllResult getAll(GetRelease request) {
    Objects.requireNonNull(request, "request");
    var response =
        requireResponse(
            support.convert(runGetRoot(GetMode.ALL, request), GetAllPayload.class, "get all"),
            "get all",
            "data");
    return new GetAllResult(
        mapReleasePayload(requireResponse(response.release(), "get all", "release"), "get all"),
        mapOrEmpty(response.values()),
        response.manifest(),
        mapHooks(response.hooks()),
        response.notes());
  }

  @Override
  public GetValuesResult getValues(GetRelease request) {
    Objects.requireNonNull(request, "request");
    var response =
        support.convert(runGetRoot(GetMode.VALUES, request), GetValuesPayload.class, "get values");
    return new GetValuesResult(mapOrEmpty(response == null ? null : response.values()));
  }

  @Override
  public GetManifestResult getManifest(GetRelease request) {
    Objects.requireNonNull(request, "request");
    var response =
        support.convert(
            runGetRoot(GetMode.MANIFEST, request), GetManifestPayload.class, "get manifest");
    return new GetManifestResult(response == null ? "" : response.manifest());
  }

  @Override
  public GetHooksResult getHooks(GetRelease request) {
    Objects.requireNonNull(request, "request");
    var response =
        support.convert(runGetRoot(GetMode.HOOKS, request), GetHooksPayload.class, "get hooks");
    return new GetHooksResult(mapHooks(response == null ? null : response.hooks()));
  }

  @Override
  public GetNotesResult getNotes(GetRelease request) {
    Objects.requireNonNull(request, "request");
    var response =
        support.convert(runGetRoot(GetMode.NOTES, request), GetNotesPayload.class, "get notes");
    return new GetNotesResult(response == null ? "" : response.notes());
  }

  @Override
  public GetMetadataResult getMetadata(GetRelease request) {
    Objects.requireNonNull(request, "request");
    var response =
        requireResponse(
            support.convert(
                runGetRoot(GetMode.METADATA, request), GetMetadataPayload.class, "get metadata"),
            "get metadata",
            "data");
    return new GetMetadataResult(
        response.name(),
        response.namespace(),
        response.revision(),
        ReleaseStatus.fromWireValue(response.status()),
        response.chart(),
        response.chartVersion(),
        response.appVersion(),
        parseTimestamp(response.deployedAt(), "get metadata", "deployedAt"));
  }

  private JsonNode runGetRoot(GetMode mode, GetRelease request) {
    var operation = "get " + mode.wireValue();
    log.debug("Get operation: mode={}, release={}", mode.wireValue(), request.releaseName());
    return support.invokeRootOrThrow(
        operation,
        bridge ->
            bridge.get(
                utf8(mode.wireValue()),
                utf8(request.releaseName()),
                support.toJsonBytes(NativeOptions.get(request), operation)));
  }

  private record ReleasePayload(NativeReleasePayload release) {}

  private record UninstallPayload(NativeReleasePayload release, String info) {}

  private record RollbackPayload(String releaseName, int revision) {}

  private record HistoryPayload(List<HistoryEntryPayload> entries) {}

  private record HistoryEntryPayload(
      int revision,
      String updated,
      String status,
      String chart,
      String chartVersion,
      String appVersion,
      String description) {}

  private record GetAllPayload(
      NativeReleasePayload release,
      Map<String, Object> values,
      String manifest,
      List<HookPayload> hooks,
      String notes) {}

  private record GetValuesPayload(Map<String, Object> values) {}

  private record GetManifestPayload(String manifest) {}

  private record GetHooksPayload(List<HookPayload> hooks) {}

  private record GetNotesPayload(String notes) {}

  private record GetMetadataPayload(
      String name,
      String namespace,
      int revision,
      String status,
      String chart,
      String chartVersion,
      String appVersion,
      String deployedAt) {}

  private record ListPayload(List<NativeReleasePayload> releases) {}

  private record TestPayload(NativeReleasePayload release, List<TestHookPayload> results) {}

  private record TestHookPayload(String name, String status) {}
}
