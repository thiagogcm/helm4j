package dev.nthings.helm4j.spi;

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

/**
 * SPI for release lifecycle and inspection operations.
 *
 * <p>Mutations return a domain value on success. Failures throw {@code HelmCommandException}; the
 * pending-on-cluster case is encoded by {@link Release#status()} (e.g. {@code PENDING_INSTALL}).
 */
public interface ReleaseGateway {

  Release install(InstallRelease request);

  Release upgrade(UpgradeRelease request);

  UninstallReport uninstall(UninstallRelease request);

  Release status(StatusRelease request);

  RollbackReport rollback(RollbackRelease request);

  ListResult<HistoryEntry> history(ReleaseHistory request);

  ListResult<Release> list(ListReleases request);

  TestResult test(TestRelease request);

  GetAllResult getAll(GetRelease request);

  GetValuesResult getValues(GetRelease request);

  GetManifestResult getManifest(GetRelease request);

  GetHooksResult getHooks(GetRelease request);

  GetNotesResult getNotes(GetRelease request);

  GetMetadataResult getMetadata(GetRelease request);
}
