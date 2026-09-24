package dev.gaphunter.firestorecompanion.pro

import dev.gaphunter.firestorecompanion.json.JsonNode
import dev.gaphunter.firestorecompanion.rest.FirestoreDocument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CollectionExporterTest {

    @Test
    fun `exports an empty document list as an empty JSON array`() {
        assertEquals("[]", CollectionExporter.toJson(emptyList()))
    }

    @Test
    fun `exports id and the raw typed-value fields wrapper, pretty-printed`() {
        val doc = FirestoreDocument(
            name = "projects/p/databases/(default)/documents/users/alice",
            id = "alice",
            fields = JsonNode.Obj(linkedMapOf("name" to JsonNode.Obj(linkedMapOf("stringValue" to JsonNode.Str("Alice"))))),
        )
        val json = CollectionExporter.toJson(listOf(doc))
        assertTrue(json.contains("\"id\": \"alice\""))
        assertTrue(json.contains("\"stringValue\": \"Alice\""))
        assertTrue(json.contains("\n")) // pretty-printed, not compact
    }

    @Test
    fun `preserves int64 precision -- integerValue stays a string, never becomes a lossy double`() {
        val doc = FirestoreDocument(
            name = "projects/p/databases/(default)/documents/users/alice",
            id = "alice",
            fields = JsonNode.Obj(
                linkedMapOf("bigCount" to JsonNode.Obj(linkedMapOf("integerValue" to JsonNode.Str("9223372036854775807")))),
            ),
        )
        val json = CollectionExporter.toJson(listOf(doc))
        assertTrue(json.contains("\"integerValue\": \"9223372036854775807\""))
    }

    @Test
    fun `exports multiple documents as separate array entries`() {
        val docs = listOf(
            FirestoreDocument("projects/p/databases/(default)/documents/users/a", "a", JsonNode.Obj(LinkedHashMap())),
            FirestoreDocument("projects/p/databases/(default)/documents/users/b", "b", JsonNode.Obj(LinkedHashMap())),
        )
        val json = CollectionExporter.toJson(docs)
        assertTrue(json.contains("\"id\": \"a\""))
        assertTrue(json.contains("\"id\": \"b\""))
    }
}
