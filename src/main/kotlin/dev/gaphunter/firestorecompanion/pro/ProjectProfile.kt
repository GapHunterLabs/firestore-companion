package dev.gaphunter.firestorecompanion.pro

/** One saved (name, service account path, project ID) combination -- Pro feature: switching between dev/staging/prod without retyping both fields every time. */
data class ProjectProfile(val name: String, val serviceAccountPath: String, val projectId: String)

/**
 * Serializes a list of [ProjectProfile] to/from a flat string list for
 * `PropertiesComponent.getList`/`setList` (which only stores
 * `List<String>`, no structured objects). Each profile becomes one
 * string; [FIELD_SEPARATOR] is a control character that can't appear
 * in a real file path or project ID typed through a text field, so a
 * name/path/projectId containing an ordinary character (including `|`
 * or `:`) round-trips safely.
 */
object ProjectProfileCodec {
    private const val FIELD_SEPARATOR = '\u0001'

    fun encode(profile: ProjectProfile): String =
        listOf(profile.name, profile.serviceAccountPath, profile.projectId).joinToString(FIELD_SEPARATOR.toString())

    /** Returns null for a malformed entry (wrong field count) rather than throwing -- a corrupted/hand-edited properties value must never crash the plugin. */
    fun decode(text: String): ProjectProfile? {
        val parts = text.split(FIELD_SEPARATOR)
        if (parts.size != 3) return null
        return ProjectProfile(name = parts[0], serviceAccountPath = parts[1], projectId = parts[2])
    }

    fun encodeAll(profiles: List<ProjectProfile>): List<String> = profiles.map(::encode)

    /** Silently drops any malformed entries rather than failing the whole list. */
    fun decodeAll(lines: List<String>): List<ProjectProfile> = lines.mapNotNull(::decode)
}
