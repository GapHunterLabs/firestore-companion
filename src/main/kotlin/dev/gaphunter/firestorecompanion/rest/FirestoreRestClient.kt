package dev.gaphunter.firestorecompanion.rest

import dev.gaphunter.firestorecompanion.json.JsonNode
import dev.gaphunter.firestorecompanion.json.JsonWriter
import dev.gaphunter.firestorecompanion.json.MinimalJsonParser
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

class FirestoreRestException(message: String) : Exception(message)

data class FirestoreDocument(val name: String, val id: String, val fields: JsonNode.Obj)

/**
 * Simple REST calls to firestore.googleapis.com/v1 -- no gRPC/heavy SDK
 * dependency, matching NEXT_BATCH_PLAN.md's explicit scope call.
 * [httpGet]/[httpPost]/[httpPatch] are injectable so tests can supply a
 * captured real Firestore REST API response fixture instead of a live
 * project -- see FirestoreRestClientTest. Never called from the EDT; the
 * tool window dispatches all of this via executeOnPooledThread.
 */
class FirestoreRestClient(
    private val projectId: String,
    private val accessToken: String,
    private val httpGet: (url: String) -> String = { url -> defaultGet(url, accessToken) },
    private val httpPost: (url: String, body: String) -> String = { url, body -> defaultPost(url, body, accessToken) },
    private val httpPatch: (url: String, body: String) -> String = { url, body -> defaultPatch(url, body, accessToken) },
) {
    private val baseUrl = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents"

    fun listRootCollectionIds(): List<String> = listCollectionIds("$baseUrl:listCollectionIds")

    /** [documentPath] is relative to the documents root, e.g. "users/abc123". */
    fun listSubcollectionIds(documentPath: String): List<String> =
        listCollectionIds("$baseUrl/$documentPath:listCollectionIds")

    private fun listCollectionIds(url: String): List<String> {
        val root = parseOrThrow(httpPost(url, "{}"))
        val ids = root.entries["collectionIds"] as? JsonNode.Arr ?: return emptyList()
        return ids.items.mapNotNull { (it as? JsonNode.Str)?.value }
    }

    /** [collectionPath] is relative to the documents root, e.g. "users" or "users/abc123/orders". */
    fun listDocuments(collectionPath: String): List<FirestoreDocument> {
        val root = parseOrThrow(httpGet("$baseUrl/$collectionPath"))
        val docs = root.entries["documents"] as? JsonNode.Arr ?: return emptyList()
        return docs.items.mapNotNull { doc ->
            val obj = doc as? JsonNode.Obj ?: return@mapNotNull null
            val name = (obj.entries["name"] as? JsonNode.Str)?.value ?: return@mapNotNull null
            val fields = obj.entries["fields"] as? JsonNode.Obj ?: JsonNode.Obj(LinkedHashMap())
            FirestoreDocument(name = name, id = name.substringAfterLast('/'), fields = fields)
        }
    }

    /** [documentName] is the document's full resource name (`FirestoreDocument.name`). */
    fun patchDocument(documentName: String, fields: JsonNode.Obj) {
        val fieldPaths = fields.entries.keys.joinToString("&") {
            "updateMask.fieldPaths=${URLEncoder.encode(it, StandardCharsets.UTF_8)}"
        }
        val url = "https://firestore.googleapis.com/v1/$documentName?$fieldPaths"
        val body = JsonWriter.write(JsonNode.Obj(linkedMapOf("fields" to fields)))
        parseOrThrow(httpPatch(url, body))
    }

    private fun parseOrThrow(body: String): JsonNode.Obj {
        val root = try {
            MinimalJsonParser.parse(body)
        } catch (e: Exception) {
            throw FirestoreRestException("Firestore returned a response that isn't valid JSON: ${e.message}")
        }
        val obj = root as? JsonNode.Obj ?: throw FirestoreRestException("Firestore response was not a JSON object")
        val error = obj.entries["error"] as? JsonNode.Obj
        if (error != null) {
            val message = (error.entries["message"] as? JsonNode.Str)?.value ?: "unknown error"
            throw FirestoreRestException("Firestore API error: $message")
        }
        return obj
    }

    companion object {
        private val client = HttpClient.newHttpClient()

        private fun defaultGet(url: String, token: String): String {
            val request = HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", "Bearer $token")
                .GET()
                .build()
            return client.send(request, HttpResponse.BodyHandlers.ofString()).body()
        }

        private fun defaultPost(url: String, body: String, token: String): String {
            val request = HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build()
            return client.send(request, HttpResponse.BodyHandlers.ofString()).body()
        }

        private fun defaultPatch(url: String, body: String, token: String): String {
            val request = HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
                .build()
            return client.send(request, HttpResponse.BodyHandlers.ofString()).body()
        }
    }
}
