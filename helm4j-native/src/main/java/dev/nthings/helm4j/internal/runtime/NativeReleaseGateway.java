package dev.nthings.helm4j.internal.runtime;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import dev.nthings.helm4j.errors.HelmException;
import dev.nthings.helm4j.internal.gateway.ReleaseGateway;
import dev.nthings.helm4j.model.ListResult;
import dev.nthings.helm4j.release.GetAllResult;
import dev.nthings.helm4j.release.GetHooksResult;
import dev.nthings.helm4j.release.GetManifestResult;
import dev.nthings.helm4j.release.GetMetadataResult;
import dev.nthings.helm4j.release.GetMode;
import dev.nthings.helm4j.release.GetNotesResult;
import dev.nthings.helm4j.release.GetRequest;
import dev.nthings.helm4j.release.GetValuesResult;
import dev.nthings.helm4j.release.HistoryEntry;
import dev.nthings.helm4j.release.HistoryRequest;
import dev.nthings.helm4j.release.InstallRequest;
import dev.nthings.helm4j.release.ReleaseFailure;
import dev.nthings.helm4j.release.ReleaseInfo;
import dev.nthings.helm4j.release.ReleaseListRequest;
import dev.nthings.helm4j.release.ReleaseOutcome;
import dev.nthings.helm4j.release.ReleasePending;
import dev.nthings.helm4j.release.ReleaseStatus;
import dev.nthings.helm4j.release.ReleaseSuccess;
import dev.nthings.helm4j.release.RollbackRequest;
import dev.nthings.helm4j.release.RollbackSuccess;
import dev.nthings.helm4j.release.StatusRequest;
import dev.nthings.helm4j.release.StatusResult;
import dev.nthings.helm4j.release.TestHookResult;
import dev.nthings.helm4j.release.TestRequest;
import dev.nthings.helm4j.release.TestResult;
import dev.nthings.helm4j.release.UninstallRequest;
import dev.nthings.helm4j.release.UninstallSuccess;
import dev.nthings.helm4j.release.UpgradeRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.jackson.databind.JsonNode;

import static dev.nthings.helm4j.internal.runtime.NativeGatewaySupport.listOrEmpty;
import static dev.nthings.helm4j.internal.runtime.NativeGatewaySupport.mapHooks;
import static dev.nthings.helm4j.internal.runtime.NativeGatewaySupport.mapOrEmpty;
import static dev.nthings.helm4j.internal.runtime.NativeGatewaySupport.mapReleasePayload;
import static dev.nthings.helm4j.internal.runtime.NativeGatewaySupport.messageOrUnknown;
import static dev.nthings.helm4j.internal.runtime.NativeGatewaySupport.operationError;
import static dev.nthings.helm4j.internal.runtime.NativeGatewaySupport.parseTimestamp;
import static dev.nthings.helm4j.internal.runtime.NativeGatewaySupport.utf8;

/** Release lifecycle and inspection operations backed by the JSON bridge. */
final class NativeReleaseGateway implements ReleaseGateway {

  private static final Logger log = LoggerFactory.getLogger(NativeReleaseGateway.class);

  private final NativeGatewaySupport support;

  NativeReleaseGateway(NativeGatewaySupport support) {
    this.support = Objects.requireNonNull(support, "support");
  }

  @Override
  public ReleaseOutcome install(InstallRequest request) {
    Objects.requireNonNull(request, "request");
    if (request.chart() == null) {
      throw new IllegalArgumentException("Install requires chart reference");
    }

    log.debug(
        "Installing release: name={}, chart={}",
        request.releaseName(),
        request.chart().asReference());
    var root =
        support.invokeRoot(
            "install",
            bridge ->
                bridge.install(
                    utf8(request.releaseName()),
                    utf8(request.chart().asReference()),
                    support.toJsonBytes(NativeOptions.install(request), "install")));

    var failure = operationError(root, "install");
    if (failure != null) {
      return new ReleaseFailure(
          messageOrUnknown(failure.message()), failure.stage(), failure.operation());
    }

    var response = support.convert(root, InstallPayload.class, "install");
    if (response == null || response.release() == null) {
      throw new HelmException(
          "Native install response missing release", "decodeResponse", "install");
    }

    var release = mapReleasePayload(response.release(), "install");

    if (release.status().isPending()) {
      return new ReleasePending(release);
    }
    return new ReleaseSuccess(release);
  }

  @Override
  public ReleaseOutcome upgrade(UpgradeRequest request) {
    Objects.requireNonNull(request, "request");
    if (request.chart() == null) {
      throw new IllegalArgumentException("Upgrade requires chart reference");
    }

    log.debug(
        "Upgrading release: name={}, chart={}",
        request.releaseName(),
        request.chart().asReference());
    var root =
        support.invokeRoot(
            "upgrade",
            bridge ->
                bridge.upgrade(
                    utf8(request.releaseName()),
                    utf8(request.chart().asReference()),
                    support.toJsonBytes(NativeOptions.upgrade(request), "upgrade")));

    var failure = operationError(root, "upgrade");
    if (failure != null) {
      return new ReleaseFailure(
          messageOrUnknown(failure.message()), failure.stage(), failure.operation());
    }

    var response = support.convert(root, ReleasePayload.class, "upgrade");
    if (response == null || response.release() == null) {
      throw new HelmException(
          "Native upgrade response missing release", "decodeResponse", "upgrade");
    }

    var release = mapReleasePayload(response.release(), "upgrade");
    if (release.status().isPending()) {
      return new ReleasePending(release);
    }
    return new ReleaseSuccess(release);
  }

  @Override
  public ReleaseOutcome uninstall(UninstallRequest request) {
    Objects.requireNonNull(request, "request");

    log.debug("Uninstalling release: name={}", request.releaseName());
    var root =
        support.invokeRoot(
            "uninstall",
            bridge ->
                bridge.uninstall(
                    utf8(request.releaseName()),
                    support.toJsonBytes(NativeOptions.uninstall(request), "uninstall")));

    var failure = operationError(root, "uninstall");
    if (failure != null) {
      return new ReleaseFailure(
          messageOrUnknown(failure.message()), failure.stage(), failure.operation());
    }

    var response = support.convert(root, UninstallPayload.class, "uninstall");
    if (response == null) {
      throw new HelmException(
          "Native uninstall response missing data", "decodeResponse", "uninstall");
    }

    var release =
        response.release() != null ? mapReleasePayload(response.release(), "uninstall") : null;
    return new UninstallSuccess(release, response.info());
  }

  @Override
  public StatusResult status(StatusRequest request) {
    Objects.requireNonNull(request, "request");

    log.debug("Getting status: name={}", request.releaseName());
    var root =
        support.invokeRootOrThrow(
            "status",
            bridge ->
                bridge.status(
                    utf8(request.releaseName()),
                    support.toJsonBytes(NativeOptions.status(request), "status")));

    var response = support.convert(root, ReleasePayload.class, "status");
    if (response == null || response.release() == null) {
      throw new HelmException("Native status response missing release", "decodeResponse", "status");
    }

    return new StatusResult(mapReleasePayload(response.release(), "status"));
  }

  @Override
  public ReleaseOutcome rollback(RollbackRequest request) {
    Objects.requireNonNull(request, "request");

    log.debug("Rolling back release: name={}", request.releaseName());
    var root =
        support.invokeRoot(
            "rollback",
            bridge ->
                bridge.rollback(
                    utf8(request.releaseName()),
                    support.toJsonBytes(NativeOptions.rollback(request), "rollback")));

    var failure = operationError(root, "rollback");
    if (failure != null) {
      return new ReleaseFailure(
          messageOrUnknown(failure.message()), failure.stage(), failure.operation());
    }

    var response = support.convert(root, RollbackPayload.class, "rollback");
    if (response == null) {
      throw new HelmException(
          "Native rollback response missing data", "decodeResponse", "rollback");
    }

    return new RollbackSuccess(response.releaseName(), response.revision());
  }

  @Override
  public ListResult<HistoryEntry> history(HistoryRequest request) {
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
  public ListResult<ReleaseInfo> list(ReleaseListRequest request) {
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
  public TestResult test(TestRequest request) {
    Objects.requireNonNull(request, "request");
    if (request.releaseName() == null) {
      throw new IllegalArgumentException("Test requires release name");
    }

    log.debug("Testing release: name={}", request.releaseName());
    var root =
        support.invokeRootOrThrow(
            "test",
            bridge ->
                bridge.test(
                    utf8(request.releaseName()),
                    support.toJsonBytes(NativeOptions.test(request), "test")));

    var response = support.convert(root, TestPayload.class, "test");
    if (response == null || response.release() == null) {
      throw new HelmException("Native test response missing release", "decodeResponse", "test");
    }

    var results =
        listOrEmpty(response.results()).stream()
            .map(r -> new TestHookResult(r.name(), r.status()))
            .toList();
    return new TestResult(mapReleasePayload(response.release(), "test"), results);
  }

  @Override
  public GetAllResult getAll(GetRequest request) {
    Objects.requireNonNull(request, "request");
    var response =
        support.convert(runGetRoot(GetMode.ALL, request), GetAllPayload.class, "get all");
    if (response == null || response.release() == null) {
      throw new HelmException(
          "Native get all response missing release", "decodeResponse", "get all");
    }
    return new GetAllResult(
        mapReleasePayload(response.release(), "get all"),
        mapOrEmpty(response.values()),
        response.manifest(),
        mapHooks(response.hooks()),
        response.notes());
  }

  @Override
  public GetValuesResult getValues(GetRequest request) {
    Objects.requireNonNull(request, "request");
    var response =
        support.convert(runGetRoot(GetMode.VALUES, request), GetValuesPayload.class, "get values");
    return new GetValuesResult(mapOrEmpty(response == null ? null : response.values()));
  }

  @Override
  public GetManifestResult getManifest(GetRequest request) {
    Objects.requireNonNull(request, "request");
    var response =
        support.convert(
            runGetRoot(GetMode.MANIFEST, request), GetManifestPayload.class, "get manifest");
    return new GetManifestResult(response == null ? "" : response.manifest());
  }

  @Override
  public GetHooksResult getHooks(GetRequest request) {
    Objects.requireNonNull(request, "request");
    var response =
        support.convert(runGetRoot(GetMode.HOOKS, request), GetHooksPayload.class, "get hooks");
    return new GetHooksResult(mapHooks(response == null ? null : response.hooks()));
  }

  @Override
  public GetNotesResult getNotes(GetRequest request) {
    Objects.requireNonNull(request, "request");
    var response =
        support.convert(runGetRoot(GetMode.NOTES, request), GetNotesPayload.class, "get notes");
    return new GetNotesResult(response == null ? "" : response.notes());
  }

  @Override
  public GetMetadataResult getMetadata(GetRequest request) {
    Objects.requireNonNull(request, "request");
    var response =
        support.convert(
            runGetRoot(GetMode.METADATA, request), GetMetadataPayload.class, "get metadata");
    if (response == null) {
      throw new HelmException(
          "Native get metadata response missing data", "decodeResponse", "get metadata");
    }
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

  private JsonNode runGetRoot(GetMode mode, GetRequest request) {
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

  private record InstallPayload(NativeReleasePayload release) {}

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
