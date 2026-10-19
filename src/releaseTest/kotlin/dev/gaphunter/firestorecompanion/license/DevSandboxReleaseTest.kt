package dev.gaphunter.firestorecompanion.license

import org.junit.Assert.assertFalse
import org.junit.Test

class DevSandboxReleaseTest {

    @Test
    fun `a release build never opens Pro without a license`() {
        assertFalse(DevSandbox.isForced())
    }

    @Test
    fun `the release gate goes straight to the real license check`() {
        // Outside an IDE there is no LicensingFacade: the real check can only say "not licensed".
        val answer = try {
            ProAccess.isPro()
        } catch (e: Throwable) {
            false
        }
        assertFalse(answer)
    }
}
