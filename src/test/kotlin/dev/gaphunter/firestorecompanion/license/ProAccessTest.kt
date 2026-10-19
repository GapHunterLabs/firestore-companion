package dev.gaphunter.firestorecompanion.license

import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProAccessTest {
    private lateinit var originalLicense: () -> Boolean?
    private lateinit var originalLegacy: () -> Boolean

    @Before
    fun remember() {
        originalLicense = ProAccess.licenseCheck
        originalLegacy = ProAccess.legacyCheck
    }

    @After
    fun restore() {
        ProAccess.licenseCheck = originalLicense
        ProAccess.legacyCheck = originalLegacy
    }

    @Test
    fun `editing needs a license on a new installation`() {
        ProAccess.licenseCheck = { false }
        ProAccess.legacyCheck = { false }
        assertFalse(ProAccess.canEdit())
        ProAccess.licenseCheck = { null }
        assertFalse(ProAccess.canEdit())
    }

    @Test
    fun `editing stays available with a license or on an installation that had it`() {
        ProAccess.licenseCheck = { true }
        ProAccess.legacyCheck = { false }
        assertTrue(ProAccess.canEdit())
        ProAccess.licenseCheck = { false }
        ProAccess.legacyCheck = { true }
        assertTrue(ProAccess.canEdit())
    }

    @Test
    fun `profiles, queries and export stay Pro only -- an installation that had editing doesn't get them`() {
        ProAccess.licenseCheck = { false }
        ProAccess.legacyCheck = { true }
        assertFalse(ProAccess.isPro())
    }

    @Test
    fun `the legacy stamp only counts before the cutoff`() {
        assertTrue(LegacyFreeFeatures.keeps(LegacyFreeFeatures.CUTOFF.minusDays(1).toString()))
        assertFalse(LegacyFreeFeatures.keeps(LegacyFreeFeatures.CUTOFF.toString()))
        assertFalse(LegacyFreeFeatures.keeps(null))
        assertFalse(LegacyFreeFeatures.keeps("not a date"))
    }
}
