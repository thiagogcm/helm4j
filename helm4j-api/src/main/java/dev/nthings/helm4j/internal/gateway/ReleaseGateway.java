package dev.nthings.helm4j.internal.gateway;

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

/** Internal release lifecycle operations exposed to the release namespace client. */
public interface ReleaseGateway {

  ReleaseResult install(InstallRequest request);

  ReleaseResult upgrade(UpgradeRequest request);

  UninstallResult uninstall(UninstallRequest request);

  StatusResult status(StatusRequest request);

  RollbackResult rollback(RollbackRequest request);

  ListResult<HistoryEntry> history(HistoryRequest request);

  ListResult<ReleaseInfo> list(ReleaseListRequest request);

  TestResult test(TestRequest request);

  GetAllResult getAll(GetRequest request);

  GetValuesResult getValues(GetRequest request);

  GetManifestResult getManifest(GetRequest request);

  GetHooksResult getHooks(GetRequest request);

  GetNotesResult getNotes(GetRequest request);

  GetMetadataResult getMetadata(GetRequest request);
}
