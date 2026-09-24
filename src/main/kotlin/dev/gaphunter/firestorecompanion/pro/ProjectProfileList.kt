package dev.gaphunter.firestorecompanion.pro

/**
 * Pure list operations on [ProjectProfile]s, kept separate from
 * [ProjectProfileStore]'s `PropertiesComponent` I/O so the add/replace/
 * remove logic is unit-testable without a running platform.
 */
object ProjectProfileList {
    /** Adds [profile], or replaces the existing entry with the same [ProjectProfile.name] (case-sensitive) -- a name is the user-facing identity of a profile. */
    fun upsert(profiles: List<ProjectProfile>, profile: ProjectProfile): List<ProjectProfile> {
        val index = profiles.indexOfFirst { it.name == profile.name }
        return if (index >= 0) profiles.toMutableList().apply { this[index] = profile } else profiles + profile
    }

    fun remove(profiles: List<ProjectProfile>, name: String): List<ProjectProfile> =
        profiles.filterNot { it.name == name }

    fun findByName(profiles: List<ProjectProfile>, name: String): ProjectProfile? =
        profiles.firstOrNull { it.name == name }
}
