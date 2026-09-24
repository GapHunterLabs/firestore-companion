package dev.gaphunter.firestorecompanion.pro

import com.intellij.ide.util.PropertiesComponent

private const val KEY_PROFILES = "dev.gaphunter.firestorecompanion.pro.profiles"
private const val KEY_ACTIVE_PROFILE = "dev.gaphunter.firestorecompanion.pro.activeProfile"

/**
 * Thin `PropertiesComponent` wrapper around [ProjectProfileList]/
 * [ProjectProfileCodec] -- Pro feature, lets a user switch between
 * dev/staging/prod service account + project ID combinations without
 * retyping both fields every time (the free tier keeps the original
 * single service-account-path/project-ID fields untouched).
 */
class ProjectProfileStore(private val properties: PropertiesComponent) {

    fun listProfiles(): List<ProjectProfile> = ProjectProfileCodec.decodeAll(properties.getList(KEY_PROFILES)?.toList() ?: emptyList())

    fun saveProfile(profile: ProjectProfile) {
        val updated = ProjectProfileList.upsert(listProfiles(), profile)
        properties.setList(KEY_PROFILES, ProjectProfileCodec.encodeAll(updated))
    }

    fun deleteProfile(name: String) {
        val updated = ProjectProfileList.remove(listProfiles(), name)
        properties.setList(KEY_PROFILES, ProjectProfileCodec.encodeAll(updated))
        if (activeProfileName() == name) properties.unsetValue(KEY_ACTIVE_PROFILE)
    }

    fun activeProfileName(): String? = properties.getValue(KEY_ACTIVE_PROFILE)

    fun activeProfile(): ProjectProfile? = activeProfileName()?.let { ProjectProfileList.findByName(listProfiles(), it) }

    fun setActiveProfile(name: String) {
        properties.setValue(KEY_ACTIVE_PROFILE, name)
    }
}
