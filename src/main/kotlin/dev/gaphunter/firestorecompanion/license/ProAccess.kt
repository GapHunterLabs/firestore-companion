package dev.gaphunter.firestorecompanion.license

/**
 * Asked at the moment of each Pro action, never once when the tool window is built, so a
 * license or trial started while the window is open works right away. Fail-closed: only an
 * explicit `true` from the license check counts. Both checks are injectable for tests.
 */
object ProAccess {
    @Volatile
    var licenseCheck: () -> Boolean? = { CheckLicense.isLicensed() }

    @Volatile
    var legacyCheck: () -> Boolean = { LegacyFreeFeatures.keepsMovedFeatures() }

    /** Saved profiles, the query builder and JSON export. */
    fun isPro(): Boolean = licenseCheck() == true

    /** Editing documents: Pro, or an installation that had it before it moved to Pro. */
    fun canEdit(): Boolean = isPro() || legacyCheck()
}
