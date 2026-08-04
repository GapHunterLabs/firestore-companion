package dev.gaphunter.firestorecompanion.auth

import dev.gaphunter.firestorecompanion.json.JsonNode
import dev.gaphunter.firestorecompanion.json.MinimalJsonParser

data class ServiceAccountCredentials(
    val projectId: String,
    val clientEmail: String,
    val privateKeyPem: String,
    val tokenUri: String,
)

class FirestoreAuthException(message: String) : Exception(message)

/**
 * Parses a GCP service account key JSON file into [ServiceAccountCredentials].
 * Every required field is looked up explicitly and reported BY NAME if
 * missing or the wrong type -- never a bare `.first { }`/`.single { }`
 * over a collection that might not contain a match. This is the direct
 * fix for the cited competitor bug: "Connection test failed:
 * java.util.NoSuchElementException: Collection contains no element
 * matching the predicate." That message tells a user nothing about
 * which field was missing or malformed; every failure path here says
 * exactly that.
 */
object ServiceAccountParser {
    fun parse(rawJson: String): ServiceAccountCredentials {
        val root = try {
            MinimalJsonParser.parse(rawJson)
        } catch (e: Exception) {
            throw FirestoreAuthException("Service account file is not valid JSON: ${e.message}")
        }
        val obj = root as? JsonNode.Obj
            ?: throw FirestoreAuthException("Service account file must contain a JSON object")

        val type = requireString(obj, "type")
        if (type != "service_account") {
            throw FirestoreAuthException(
                "Expected \"type\": \"service_account\", found \"$type\" -- this doesn't look like a GCP service account key file"
            )
        }

        return ServiceAccountCredentials(
            projectId = requireString(obj, "project_id"),
            clientEmail = requireString(obj, "client_email"),
            privateKeyPem = requireString(obj, "private_key"),
            tokenUri = optionalString(obj, "token_uri") ?: "https://oauth2.googleapis.com/token",
        )
    }

    private fun requireString(obj: JsonNode.Obj, key: String): String {
        val value = obj.entries[key]
            ?: throw FirestoreAuthException("Service account JSON is missing required field \"$key\"")
        val str = value as? JsonNode.Str
            ?: throw FirestoreAuthException("Service account JSON field \"$key\" must be a string")
        if (str.value.isBlank()) {
            throw FirestoreAuthException("Service account JSON field \"$key\" is empty")
        }
        return str.value
    }

    private fun optionalString(obj: JsonNode.Obj, key: String): String? =
        (obj.entries[key] as? JsonNode.Str)?.value?.takeIf { it.isNotBlank() }
}
