package dev.gaphunter.firestorecompanion.rest

import dev.gaphunter.firestorecompanion.json.JsonNode
import org.junit.Assert.assertEquals
import org.junit.Test

class FirestoreValueFormatterTest {
    @Test
    fun `formats primitive typed values`() {
        assertEquals("Alice", FirestoreValueFormatter.format(obj("stringValue" to JsonNode.Str("Alice"))))
        assertEquals("30", FirestoreValueFormatter.format(obj("integerValue" to JsonNode.Str("30"))))
        assertEquals("true", FirestoreValueFormatter.format(obj("booleanValue" to JsonNode.Bool(true))))
        assertEquals("null", FirestoreValueFormatter.format(obj("nullValue" to JsonNode.Null)))
    }

    @Test
    fun `formats a map value recursively`() {
        val city = obj("stringValue" to JsonNode.Str("Springfield"))
        val mapValue = obj("mapValue" to obj("fields" to JsonNode.Obj(linkedMapOf("city" to city))))
        assertEquals("{city: Springfield}", FirestoreValueFormatter.format(mapValue))
    }

    @Test
    fun `formats an array value`() {
        val values = JsonNode.Arr(listOf(obj("integerValue" to JsonNode.Str("1")), obj("integerValue" to JsonNode.Str("2"))))
        val arrayValue = obj("arrayValue" to obj("values" to values))
        assertEquals("[1, 2]", FirestoreValueFormatter.format(arrayValue))
    }

    @Test
    fun `formats an empty fields object as empty braces, never throws`() {
        assertEquals("{}", FirestoreValueFormatter.formatFields(JsonNode.Obj(LinkedHashMap())))
        assertEquals("{}", FirestoreValueFormatter.formatFields(null))
    }

    private fun obj(vararg pairs: Pair<String, JsonNode>): JsonNode.Obj = JsonNode.Obj(linkedMapOf(*pairs))
}
