package dev.nthings.helm4j.release;

/** Domain result hierarchy for release upgrade outcomes. */
public sealed interface UpgradeResult permits UpgradeSuccess, UpgradePending, UpgradeFailure {}
