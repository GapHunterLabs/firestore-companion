package dev.gaphunter.firestorecompanion.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectProfileTest {

    @Test
    fun `a profile round-trips through encode and decode`() {
        val profile = ProjectProfile("prod", "/home/joel/sa-prod.json", "acme-prod")
        val decoded = ProjectProfileCodec.decode(ProjectProfileCodec.encode(profile))
        assertEquals(profile, decoded)
    }

    @Test
    fun `encode-decode preserves fields containing ordinary punctuation`() {
        // Colons, pipes and Windows drive letters are real characters a
        // path or project ID can contain -- must not be confused with a
        // delimiter.
        val profile = ProjectProfile("dev: local|test", "C:\\Users\\joel\\sa.json", "acme-dev-1")
        val decoded = ProjectProfileCodec.decode(ProjectProfileCodec.encode(profile))
        assertEquals(profile, decoded)
    }

    @Test
    fun `a malformed entry decodes to null instead of throwing`() {
        assertNull(ProjectProfileCodec.decode("not-enough-fields"))
    }

    @Test
    fun `decodeAll silently drops malformed entries`() {
        val good = ProjectProfile("prod", "/sa.json", "acme")
        val lines = listOf(ProjectProfileCodec.encode(good), "corrupted-entry")
        assertEquals(listOf(good), ProjectProfileCodec.decodeAll(lines))
    }

    @Test
    fun `upsert adds a new profile`() {
        val profiles = ProjectProfileList.upsert(emptyList(), ProjectProfile("dev", "a.json", "p1"))
        assertEquals(1, profiles.size)
    }

    @Test
    fun `upsert replaces an existing profile with the same name`() {
        val original = listOf(ProjectProfile("dev", "old.json", "p1"))
        val updated = ProjectProfileList.upsert(original, ProjectProfile("dev", "new.json", "p1"))
        assertEquals(1, updated.size)
        assertEquals("new.json", updated[0].serviceAccountPath)
    }

    @Test
    fun `remove drops only the named profile`() {
        val profiles = listOf(ProjectProfile("dev", "a.json", "p1"), ProjectProfile("prod", "b.json", "p2"))
        val updated = ProjectProfileList.remove(profiles, "dev")
        assertEquals(listOf(ProjectProfile("prod", "b.json", "p2")), updated)
    }

    @Test
    fun `findByName returns null when nothing matches`() {
        assertNull(ProjectProfileList.findByName(emptyList(), "dev"))
    }

    @Test
    fun `findByName is exact, not a substring match`() {
        val profiles = listOf(ProjectProfile("dev-old", "a.json", "p1"))
        assertNull(ProjectProfileList.findByName(profiles, "dev"))
        assertTrue(ProjectProfileList.findByName(profiles, "dev-old") != null)
    }
}
