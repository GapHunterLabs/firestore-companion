package dev.gaphunter.firestorecompanion.rest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Never touches a real Firestore project -- GET/POST/PATCH are injected
 * with fake transports returning fixtures matching the real, documented
 * Firestore REST API v1 response shapes (typed-value wrappers for
 * fields, `collectionIds` for :listCollectionIds, `documents` for a
 * collection listing).
 */
class FirestoreRestClientTest {
    // Real Firestore REST API v1 document-list response shape.
    private val documentsFixture = """
        {
          "documents": [
            {
              "name": "projects/acme-corp-prod/databases/(default)/documents/users/alice",
              "fields": {
                "name": { "stringValue": "Alice" },
                "age": { "integerValue": "30" },
                "active": { "booleanValue": true },
                "address": { "mapValue": { "fields": { "city": { "stringValue": "Springfield" } } } }
              },
              "createTime": "2026-01-01T00:00:00.000000Z",
              "updateTime": "2026-01-02T00:00:00.000000Z"
            },
            {
              "name": "projects/acme-corp-prod/databases/(default)/documents/users/bob",
              "fields": {
                "name": { "stringValue": "Bob" }
              }
            }
          ]
        }
    """.trimIndent()

    private val collectionIdsFixture = """{"collectionIds": ["users", "orders"]}"""

    @Test
    fun `lists root collection ids`() {
        var requestedUrl: String? = null
        val client = FirestoreRestClient(
            projectId = "acme-corp-prod",
            accessToken = "fake-token",
            httpGet = { error("not expected") },
            httpPost = { url, _ -> requestedUrl = url; collectionIdsFixture },
            httpPatch = { _, _ -> error("not expected") },
        )
        val ids = client.listRootCollectionIds()
        assertEquals(listOf("users", "orders"), ids)
        assertTrue(requestedUrl!!.endsWith(":listCollectionIds"))
        assertTrue(requestedUrl!!.contains("projects/acme-corp-prod/databases/(default)/documents"))
    }

    @Test
    fun `lists documents with correctly typed fields`() {
        var requestedUrl: String? = null
        val client = FirestoreRestClient(
            projectId = "acme-corp-prod",
            accessToken = "fake-token",
            httpGet = { url -> requestedUrl = url; documentsFixture },
            httpPost = { _, _ -> error("not expected") },
            httpPatch = { _, _ -> error("not expected") },
        )
        val documents = client.listDocuments("users")
        assertEquals(2, documents.size)
        assertEquals("alice", documents[0].id)
        assertEquals("Alice", FirestoreValueFormatter.format(documents[0].fields.entries.getValue("name")))
        assertEquals("30", FirestoreValueFormatter.format(documents[0].fields.entries.getValue("age")))
        assertTrue(requestedUrl!!.endsWith("/documents/users"))
    }

    @Test
    fun `lists subcollection ids for a specific document`() {
        var requestedUrl: String? = null
        val client = FirestoreRestClient(
            projectId = "acme-corp-prod",
            accessToken = "fake-token",
            httpGet = { error("not expected") },
            httpPost = { url, _ -> requestedUrl = url; """{"collectionIds": ["orders"]}""" },
            httpPatch = { _, _ -> error("not expected") },
        )
        val ids = client.listSubcollectionIds("users/alice")
        assertEquals(listOf("orders"), ids)
        assertTrue(requestedUrl!!.contains("/documents/users/alice:listCollectionIds"))
    }

    @Test
    fun `a document with no fields at all does not crash`() {
        val client = FirestoreRestClient(
            projectId = "p",
            accessToken = "t",
            httpGet = { """{"documents": [{"name": "projects/p/databases/(default)/documents/users/empty"}]}""" },
            httpPost = { _, _ -> error("not expected") },
            httpPatch = { _, _ -> error("not expected") },
        )
        val documents = client.listDocuments("users")
        assertEquals(1, documents.size)
        assertTrue(documents[0].fields.entries.isEmpty())
    }

    @Test
    fun `an empty collection returns an empty list, not a crash`() {
        val client = FirestoreRestClient(
            projectId = "p",
            accessToken = "t",
            httpGet = { """{}""" },
            httpPost = { _, _ -> error("not expected") },
            httpPatch = { _, _ -> error("not expected") },
        )
        assertTrue(client.listDocuments("users").isEmpty())
    }

    @Test
    fun `a firestore api error response is surfaced clearly`() {
        val client = FirestoreRestClient(
            projectId = "p",
            accessToken = "bad-token",
            httpGet = { """{"error": {"code": 401, "message": "Request had invalid authentication credentials."}}""" },
            httpPost = { _, _ -> error("not expected") },
            httpPatch = { _, _ -> error("not expected") },
        )
        val exception = assertThrows(FirestoreRestException::class.java) {
            client.listDocuments("users")
        }
        assertTrue(exception.message!!.contains("invalid authentication credentials"))
    }

    @Test
    fun `patch sends the fields and update mask for each key`() {
        var patchedUrl: String? = null
        var patchedBody: String? = null
        val client = FirestoreRestClient(
            projectId = "p",
            accessToken = "t",
            httpGet = { error("not expected") },
            httpPost = { _, _ -> error("not expected") },
            httpPatch = { url, body -> patchedUrl = url; patchedBody = body; """{"name": "projects/p/databases/(default)/documents/users/alice"}""" },
        )
        val fields = dev.gaphunter.firestorecompanion.json.JsonNode.Obj(
            linkedMapOf("name" to dev.gaphunter.firestorecompanion.json.JsonNode.Obj(linkedMapOf("stringValue" to dev.gaphunter.firestorecompanion.json.JsonNode.Str("Alicia"))))
        )
        client.patchDocument("projects/p/databases/(default)/documents/users/alice", fields)
        assertTrue(patchedUrl!!.contains("updateMask.fieldPaths=name"))
        assertTrue(patchedBody!!.contains("Alicia"))
    }
}
