package dev.nthings.helm4j.release;

/** Conflict/apply strategy for release mutation operations. */
public enum ApplyStrategy {
  SERVER_SIDE_APPLY(true, false),
  SERVER_SIDE_APPLY_FORCE_CONFLICTS(true, true),
  CLIENT_SIDE_APPLY(false, false);

  private final boolean serverSideApply;
  private final boolean forceConflicts;

  ApplyStrategy(boolean serverSideApply, boolean forceConflicts) {
    this.serverSideApply = serverSideApply;
    this.forceConflicts = forceConflicts;
  }

  public boolean serverSideApply() {
    return serverSideApply;
  }

  public boolean forceConflicts() {
    return forceConflicts;
  }
}
